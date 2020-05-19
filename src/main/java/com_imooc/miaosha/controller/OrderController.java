package com_imooc.miaosha.controller;

import com_imooc.miaosha.domain.MiaoshaUser;
import com_imooc.miaosha.domain.OrderInfo;
import com_imooc.miaosha.redis.RedisService;
import com_imooc.miaosha.result.CodeMsg;
import com_imooc.miaosha.result.Result;
import com_imooc.miaosha.service.GoodsService;
import com_imooc.miaosha.service.MiaoshaUserService;
import com_imooc.miaosha.service.OrderService;
import com_imooc.miaosha.vo.GoodsVo;
import com_imooc.miaosha.vo.OrderDetailVo;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletResponse;

/**
 * 获取秒杀订单的详细信息
 */

@Controller
@RequestMapping("/order")
public class OrderController {

    @Autowired
    MiaoshaUserService userService;

    @Autowired
    RedisService redisService;

    @Autowired
    OrderService orderService;

    @Autowired
    GoodsService goodsService;

    @RequestMapping("/detail")
    @ResponseBody
    public Result<OrderDetailVo> info(Model model, @RequestParam("orderId") long orderId, HttpServletResponse response,
                                      @CookieValue(value = MiaoshaUserService.COOKI_NAME_TOKEN,required = false) String cookieToken,
                                      @RequestParam(value = MiaoshaUserService.COOKI_NAME_TOKEN,required = false) String paramToken) {
        if (StringUtils.isEmpty(cookieToken) && StringUtils.isEmpty(paramToken)) {
            return Result.error(CodeMsg.SESSION_ERROR);//token不存在或失效
        }
        String token = StringUtils.isEmpty(paramToken) ? cookieToken : paramToken;
        MiaoshaUser user = userService.getByToken(response, token);//从token中读用户信息

        OrderInfo order = orderService.getOrderById(orderId);
        if (order == null) {
            return Result.error(CodeMsg.ORDER_NOT_EXIST);
        }
        long goodsId = order.getGoodsId();
        GoodsVo goods = goodsService.getGoodsVoByGoodsId(goodsId);
        OrderDetailVo vo = new OrderDetailVo();
        vo.setOrder(order);
        vo.setGoods(goods);
        return Result.success(vo);
    }

}
