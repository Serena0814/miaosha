package com_imooc.miaosha.controller;


import com_imooc.miaosha.domain.MiaoshaUser;
import com_imooc.miaosha.domain.User;
import com_imooc.miaosha.rabbitmq.MQSender;
import com_imooc.miaosha.redis.MiaoshaUserKey;
import com_imooc.miaosha.redis.RedisService;
import com_imooc.miaosha.redis.UserKey;
import com_imooc.miaosha.result.Result;
import com_imooc.miaosha.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/demo")
public class DemoController {

    @Autowired
    UserService userService;

    @Autowired
    RedisService redisService;

    @Autowired
    MQSender sender;

    /*
     显示"Hello World!"字符串
     */
    @RequestMapping("/")
    @ResponseBody
    String home() {
        return "Hello World!";
    }

    /*
     根据application.properties中对thymeleaf的配置可知：
     该方法将返回的是/templates/hello.html页面
     并且将Joshua填入进html页面的name中
     */
    @RequestMapping("/thymeleaf")
    public String thymeleaf(Model model) {
        model.addAttribute("name", "Joshua");
        return "hello";
    }

    /*
     测试mysql事务操作
     */
    @RequestMapping("/db/tx")
    @ResponseBody
    public Result<Boolean> dbTx() {
        userService.tx();
        return Result.success(true);
    }

    /*
     测试redis的set功能
     */
    @RequestMapping("/redis/set")
    @ResponseBody
    public Result<Boolean> redisSet() {
        MiaoshaUser user = new MiaoshaUser();
        user.setId(123L);
        user.setNickname("zzz");
        redisService.set(MiaoshaUserKey.getById, "" + 123, user);//MiaoshaUserKey:id123
        return Result.success(true);
    }

    /*
     测试redis的get功能
     */
    @RequestMapping("/redis/get")
    @ResponseBody
    public Result<MiaoshaUser> redisGet() {
        MiaoshaUser user = redisService.get(MiaoshaUserKey.getById, "" + 123, MiaoshaUser.class);
        return Result.success(user);
    }

    /*
     测试mq的功能
     */
    @RequestMapping("/mq")
    @ResponseBody
    public Result<String> mq() {
        sender.send("direct!");
        return Result.success("Hello，world");
    }

    @RequestMapping("/mq/topic")
    @ResponseBody
    public Result<String> topic() {
        sender.sendTopic("topic!");
        return Result.success("Hello，world");
    }

    @RequestMapping("/mq/fanout")
    @ResponseBody
    public Result<String> fanout() {
        sender.sendFanout("fanout!");
        return Result.success("Hello，world");
    }

    @RequestMapping("/mq/header")
    @ResponseBody
    public Result<String> header() {
        sender.sendHeader("hello,imooc");
        sender.sendHeader2("hello,imooc2");
        sender.sendHeader3("hello,imooc3");
        return Result.success("Hello，world");
    }
}
