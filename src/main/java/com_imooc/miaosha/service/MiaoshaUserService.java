package com_imooc.miaosha.service;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

import com_imooc.miaosha.dao.MiaoshaUserDao;
import com_imooc.miaosha.domain.MiaoshaUser;
import com_imooc.miaosha.exception.GlobalException;
import com_imooc.miaosha.redis.MiaoshaUserKey;
import com_imooc.miaosha.redis.RedisService;
import com_imooc.miaosha.result.CodeMsg;
import com_imooc.miaosha.util.MD5Util;
import com_imooc.miaosha.util.UUIDUtil;
import com_imooc.miaosha.vo.LoginVo;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;


@Service
public class MiaoshaUserService {


    public static final String COOKI_NAME_TOKEN = "token";

    @Autowired
    MiaoshaUserDao miaoshaUserDao;

    @Autowired
    RedisService redisService;


    public MiaoshaUser getById(long id) {
        //取缓存
        MiaoshaUser user = redisService.get(MiaoshaUserKey.getById, "" + id, MiaoshaUser.class);
        if (user != null) {
            return user;
        }
        //取数据库
        user = miaoshaUserDao.getById(id);
        if (user != null) {
            redisService.set(MiaoshaUserKey.getById, "" + id, user);
        }
        return user;
    }

    public MiaoshaUser getByToken(HttpServletResponse response, String token) {
        if (StringUtils.isEmpty(token)) {
            return null;
        }
        MiaoshaUser user = redisService.get(MiaoshaUserKey.token, token, MiaoshaUser.class);
        // 如果用户不为空则延长有效期
        if (user != null) {
            addCookie(response, token, user);
        }
        return user;
    }

    // http://blog.csdn.net/tTU1EvLDeLFq5btqiK/article/details/78693323
    public boolean updatePassword(String token, long id, String formPass) {
        //取user
        MiaoshaUser user = getById(id);
        if (user == null) {
            throw new GlobalException(CodeMsg.MOBILE_NOT_EXIST);
        }
        //更新数据库
        MiaoshaUser toBeUpdate = new MiaoshaUser();
        toBeUpdate.setId(id);
        toBeUpdate.setPassword(MD5Util.formPassToDBPass(formPass, user.getSalt()));
        miaoshaUserDao.update(toBeUpdate);
        //处理缓存,对象缓存一定要更新redis，否则会发生数据不一致的情况
        //这也说明了在service中调用其它的对象动作要调用service，不要调用别人的dao，
        // 因为别人的service可能有缓存，而别人的dao是直接在数据库中操作
        redisService.delete(MiaoshaUserKey.getById, "" + id);
        user.setPassword(toBeUpdate.getPassword());
        redisService.set(MiaoshaUserKey.token, token, user);
        return true;
    }

    public String login(HttpServletResponse response, LoginVo loginVo) {
        if (loginVo == null) {
            throw new GlobalException(CodeMsg.SERVER_ERROR);// 直接抛全局异常，在handler中会集中处理
        }
        String mobile = loginVo.getMobile();
        String formPass = loginVo.getPassword();
        // 判断手机号是否存在
        MiaoshaUser user = getById(Long.parseLong(mobile));
        if (user == null) {
            throw new GlobalException(CodeMsg.MOBILE_NOT_EXIST);
        }
        // 验证密码
        String dbPass = user.getPassword();
        String saltDB = user.getSalt();
        String calcPass = MD5Util.formPassToDBPass(formPass, saltDB);
        if (!calcPass.equals(dbPass)) {
            throw new GlobalException(CodeMsg.PASSWORD_ERROR);
        }
        // 更新数据库中的信息：login_count和last_login_date
        user.setLastLoginDate(new Date());
        user.setLoginCount(user.getLoginCount() + 1);
        miaoshaUserDao.update(user);
        // 通过UUID生成cookie
        String token = UUIDUtil.uuid();
        addCookie(response, token, user);
        return token;
    }

    public String register(HttpServletResponse response, LoginVo loginVo) {
        if (loginVo == null) {
            throw new GlobalException(CodeMsg.SERVER_ERROR);
        }
        String mobile = loginVo.getMobile();
        String formPass = loginVo.getPassword();
        // 判断手机号是否存在
        MiaoshaUser user = getById(Long.parseLong(mobile));
        if (user != null) {
            throw new GlobalException(CodeMsg.MOBILE_EXIST);
        }
        // 生成salt并计算得到密码
        String saltDB = UUIDUtil.uuid().substring(0,6);
        String calcPass = MD5Util.formPassToDBPass(formPass, saltDB);
        // 插入到数据库中
        user = new MiaoshaUser();
        user.setId(Long.parseLong(mobile));
        user.setNickname("nickname");
        user.setSalt(saltDB);
        user.setPassword(calcPass);
        user.setRegisterDate(new Date());
        user.setLastLoginDate(new Date());
        user.setLoginCount(1);
        miaoshaUserDao.insert(user);
        // 通过UUID生成cookie
        String token = UUIDUtil.uuid();
        addCookie(response, token, user);
        return token;
    }

    private void addCookie(HttpServletResponse response, String token, MiaoshaUser user) {
        // 把token信息写到缓存中,在redis中管理session
        redisService.set(MiaoshaUserKey.token, token, user);
        // 在cookie中放入名为“token” 值为token的字段
        Cookie cookie = new Cookie(COOKI_NAME_TOKEN, token);
        // 这里把cookie的token和Redis设为一致的有效期
        cookie.setMaxAge(MiaoshaUserKey.token.expireSeconds());
        // "/"表示可在同一应用服务器内共享cookie
        cookie.setPath("/");
        response.addCookie(cookie);
    }
}
