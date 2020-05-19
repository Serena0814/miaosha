package com_imooc.miaosha.controller;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import com_imooc.miaosha.redis.RedisService;
import com_imooc.miaosha.result.Result;
import com_imooc.miaosha.service.MiaoshaUserService;
import com_imooc.miaosha.vo.LoginVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * 该Controller类中的定义了以下方法：
 * 1.调用 /login/to_login 后返回 login.html页面，可以选择注册或登录
 * 2.若点击’登录‘则调用 /login/do_login
 * 3.若点击‘注册’则调用 /login/do_register
 * 4.登录或注册成功后都会进入到 /goods/to_list 方法 (转到GoodsController去
 */

@Controller
@RequestMapping("/login")
public class LoginController {

    private static Logger log = LoggerFactory.getLogger(LoginController.class);

    @Autowired
    MiaoshaUserService miaoshaUserService;

    @Autowired
    RedisService redisService;

    @RequestMapping("/to_login")
    public String toLogin() {
        return "login";
    }

    @RequestMapping("/do_login")
    @ResponseBody
    public Result<String> doLogin(HttpServletResponse response, @Valid LoginVo loginVo) {//注意这边有个valid，那么这个类里面需要设置一系列注解
        log.info(loginVo.toString());
        // 登录
        String token = miaoshaUserService.login(response, loginVo);
        return Result.success(token);
    }

    @RequestMapping("/do_register")
    @ResponseBody
    public Result<String> doRegister(HttpServletResponse response, @Valid LoginVo loginVo) {
        log.info(loginVo.toString());
        // 注册
        String token = miaoshaUserService.register(response, loginVo);
        return Result.success(token);
    }
}
