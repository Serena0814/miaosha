package com_imooc.miaosha.controller;

import com_imooc.miaosha.domain.MiaoshaUser;
import com_imooc.miaosha.redis.RedisService;
import com_imooc.miaosha.result.Result;
import com_imooc.miaosha.service.MiaoshaUserService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import javax.servlet.http.HttpServletResponse;


@Controller
@RequestMapping("/user")
public class UserController {

    @Autowired
    MiaoshaUserService userService;

    @Autowired
    RedisService redisService;


    /**
     * 根据token获取用户信息
     * 由于token可能是浏览器cookie中自带的，也可能是作为参数传进来的
     * 优先选取参数传进来的token
     */
    @RequestMapping("/info")
    @ResponseBody
    public Result<MiaoshaUser> info(HttpServletResponse response,
                                    @CookieValue(value = MiaoshaUserService.COOKI_NAME_TOKEN,required = false) String cookieToken,
                                    @RequestParam(value = MiaoshaUserService.COOKI_NAME_TOKEN,required = false) String paramToken) {
        if (StringUtils.isEmpty(cookieToken) && StringUtils.isEmpty(paramToken)) {
            return Result.success(null);
        }
        String token = StringUtils.isEmpty(paramToken)?cookieToken:paramToken;
        MiaoshaUser user = userService.getByToken(response,token);//从token中读用户信息
        return Result.success(user);
    }
}
