package com_imooc.miaosha.controller;

import com_imooc.miaosha.domain.MiaoshaUser;
import com_imooc.miaosha.redis.GoodsKey;
import com_imooc.miaosha.redis.RedisService;
import com_imooc.miaosha.result.CodeMsg;
import com_imooc.miaosha.result.Result;
import com_imooc.miaosha.service.GoodsService;
import com_imooc.miaosha.service.MiaoshaUserService;
import com_imooc.miaosha.vo.GoodsDetailVo;
import com_imooc.miaosha.vo.GoodsVo;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.spring5.view.ThymeleafViewResolver;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * 该Controller类中的定义了以下方法：
 * 1./goods/to_list 通过页面缓存显示 goods_list.html
 * 2.当点击某个商品的详情时，跳转到详情页面 /goods_detail.htm，页面调用 /goods/detail/goodsid
 * 3.页面显示调用结果，可能是秒杀没开始、秒杀进行中、秒杀已结束
 * 4.若秒杀进行中，则前端调用 /miaosha/verifyCode 获取验证码图片    (转到MiaoshaController中
 */

@Controller
@RequestMapping("/goods")
public class GoodsController {

    @Autowired
    MiaoshaUserService userService;

    @Autowired
    RedisService redisService;

    @Autowired
    GoodsService goodsService;

    @Autowired
    ThymeleafViewResolver thymeleafViewResolver;


   /* @RequestMapping("/to_list")
    public String list(Model model, HttpServletResponse response,
                       @CookieValue(value = MiaoshaUserService.COOKI_NAME_TOKEN,required = false) String cookieToken,
                       @RequestParam(value = MiaoshaUserService.COOKI_NAME_TOKEN,required = false) String paramToken) {
        if (StringUtils.isEmpty(cookieToken) && StringUtils.isEmpty(paramToken)) {
            return "login";
        }
        String token = StringUtils.isEmpty(paramToken)?cookieToken:paramToken;
        MiaoshaUser user = userService.getByToken(response,token);//从token中读用户信息
        model.addAttribute("user", user); //将user对象和goods_list.html页面中的user“关联”
        //查询商品列表
        List<GoodsVo> goodsList = goodsService.listGoodsVo();
        model.addAttribute("goodsList", goodsList);
        return "goods_list"; //返回goods_list.html
    }*/


    // 改成了页面缓存
    @RequestMapping(value = "/to_list", produces = "text/html")
    @ResponseBody
    public String list(Model model, HttpServletResponse response, HttpServletRequest request,
                       @CookieValue(value = MiaoshaUserService.COOKI_NAME_TOKEN, required = false) String cookieToken,
                       @RequestParam(value = MiaoshaUserService.COOKI_NAME_TOKEN, required = false) String paramToken) {
        if (StringUtils.isEmpty(cookieToken) && StringUtils.isEmpty(paramToken)) {
            return "login";
        }
        String token = StringUtils.isEmpty(paramToken) ? cookieToken : paramToken;
        MiaoshaUser user = userService.getByToken(response, token);//从token中读用户信息
        model.addAttribute("user", user); //将user对象和goods_list.html页面中的user“关联”
        // 取缓存,若缓存中存在则直接返回
        String html = redisService.get(GoodsKey.getGoodsList, "", String.class);
        if (!StringUtils.isEmpty(html)) {
            return html;
        }
        // 查询秒杀商品列表
        List<GoodsVo> goodsList = goodsService.listGoodsVo();
        model.addAttribute("goodsList", goodsList);
        //  return "goods_list"; //返回goods_list.html,由springBoot渲染

        //手动渲染，然后加入到缓存中
        WebContext ctx = new WebContext(request, response,
                request.getServletContext(), request.getLocale(), model.asMap());
        html = thymeleafViewResolver.getTemplateEngine().process("goods_list", ctx);
        if (!StringUtils.isEmpty(html)) { //如果html不为空，先存入redis中
            redisService.set(GoodsKey.getGoodsList, "", html);
        }
        return html;
    }

    //页面静态化，前后端分离
    //该方法展示的页面是在/resources/static/goods_detail.htm
    @RequestMapping(value = "/detail/{goodsId}")
    @ResponseBody
    public Result<GoodsDetailVo> detail(@PathVariable("goodsId") long goodsId, HttpServletResponse response,
                                        @CookieValue(value = MiaoshaUserService.COOKI_NAME_TOKEN, required = false) String cookieToken,
                                        @RequestParam(value = MiaoshaUserService.COOKI_NAME_TOKEN, required = false) String paramToken) {
        if (StringUtils.isEmpty(cookieToken) && StringUtils.isEmpty(paramToken)) {
            return Result.error(CodeMsg.SESSION_ERROR);//token不存在或失效
        }
        String token = StringUtils.isEmpty(paramToken) ? cookieToken : paramToken;
        MiaoshaUser user = userService.getByToken(response, token);//从token中读用户信息
        if (user == null) {
            return Result.error(CodeMsg.User_Not_Login);
        }
        GoodsVo goods = goodsService.getGoodsVoByGoodsId(goodsId);

        long startAt = goods.getStartDate().getTime();//转换成毫秒值
        long endAt = goods.getEndDate().getTime();
        long now = System.currentTimeMillis();

        int miaoshaStatus = 0;
        int remainSeconds = 0;
        if (now < startAt) {//秒杀还没开始，倒计时
            miaoshaStatus = 0;
            remainSeconds = (int) ((startAt - now) / 1000);
        } else if (now > endAt) {//秒杀已经结束
            miaoshaStatus = 2;
            remainSeconds = -1;
        } else {//秒杀进行中
            miaoshaStatus = 1;
            remainSeconds = 0;
        }
        System.out.println("success");
        GoodsDetailVo vo = new GoodsDetailVo();
        vo.setGoods(goods);
        vo.setUser(user);
        vo.setRemainSeconds(remainSeconds);
        vo.setMiaoshaStatus(miaoshaStatus);
        return Result.success(vo);

    }

}
