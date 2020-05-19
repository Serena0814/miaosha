package com_imooc.miaosha.controller;

import com_imooc.miaosha.access.AccessLimit;
import com_imooc.miaosha.domain.MiaoshaOrder;
import com_imooc.miaosha.domain.MiaoshaUser;
import com_imooc.miaosha.domain.OrderInfo;
import com_imooc.miaosha.rabbitmq.MQSender;
import com_imooc.miaosha.rabbitmq.MiaoshaMessage;
import com_imooc.miaosha.redis.*;
import com_imooc.miaosha.result.CodeMsg;
import com_imooc.miaosha.result.Result;
import com_imooc.miaosha.service.GoodsService;
import com_imooc.miaosha.service.MiaoshaService;
import com_imooc.miaosha.service.MiaoshaUserService;
import com_imooc.miaosha.service.OrderService;
import com_imooc.miaosha.vo.GoodsVo;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.awt.image.BufferedImage;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;

/**
 * 该Controller类中的定义了以下方法：
 * 1.系统初始化的时候把数据库中的库存加载进缓存中
 * 2.秒杀的过程：
 *   (1)前端通过 /miaosha/verifyCode 获取验证码图片   (当再次点击验证码图片前端会再次调用该端口刷新验证码
 *   (2)用户输入验证码后点击“立即秒杀”，前端调用 /miaosha/path 验证验证码并获得秒杀路径 （这边有限流防刷技术）
 *   (3)前端自动调用 /miaosha/{path}/do_miaosha 验证path后真正开始进行秒杀
 *   (4)结束后跳转到秒杀结果 /miaosha/result
 *   (5)若前端显示秒杀成功后，会弹出小框选择是否查看订单
 *   (6)若点击“确定”则跳转至 /order_detail 页面，前端再调用 /order/detail 获取详细信息 (转到OrrderController中
 * 3.秒杀结束后可以调用 /miaosha/reset 还原缓存和数据库
 */

@Controller
@RequestMapping("/miaosha")
public class MiaoshaController implements InitializingBean {

    @Autowired
    MiaoshaUserService userService;

    @Autowired
    RedisService redisService;

    @Autowired
    GoodsService goodsService;

    @Autowired
    OrderService orderService;

    @Autowired
    MiaoshaService miaoshaService;

    @Autowired
    MQSender sender;

    private HashMap<Long, Boolean> localOverMap = new HashMap<Long, Boolean>();

    /**
     * 系统初始化,初始化的时候把数据库中的库存加载进缓存中
     */
    public void afterPropertiesSet() {
        List<GoodsVo> goodsList = goodsService.listGoodsVo();
        if (goodsList == null) {
            return;
        }
        for (GoodsVo goods : goodsList) {
            redisService.set(GoodsKey.getMiaoshaGoodsStock, "" + goods.getId(), goods.getStockCount());
            localOverMap.put(goods.getId(), false);
        }
    }

    //秒杀接口地址隐藏
    @RequestMapping(value = "/{path}/do_miaosha", method = RequestMethod.POST)
    @ResponseBody
    public Result<Integer> miaosha( @RequestParam("goodsId") long goodsId, HttpServletResponse response,
                                   @CookieValue(value = MiaoshaUserService.COOKI_NAME_TOKEN,required = false) String cookieToken,
                                   @RequestParam(value = MiaoshaUserService.COOKI_NAME_TOKEN,required = false) String paramToken,
                                   @PathVariable("path") String path) {
        if (StringUtils.isEmpty(cookieToken) && StringUtils.isEmpty(paramToken)) {
            return Result.error(CodeMsg.SESSION_ERROR);//token不存在或失效
        }
        String token = StringUtils.isEmpty(paramToken) ? cookieToken : paramToken;
        MiaoshaUser user = userService.getByToken(response, token);//从token中读用户信息

        //验证path
        boolean check = miaoshaService.checkPath(user, goodsId, path);
        if (!check) {
            return Result.error(CodeMsg.REQUEST_ILLEGAL);
        }

        //内存标记，减少redis访问
        boolean over = localOverMap.get(goodsId);
        if (over) {//库存减去10后不必要访问redis，访问map即可
            return Result.error(CodeMsg.MIAO_SHA_OVER);
        }
        //预减库存
        long stock = redisService.decr(GoodsKey.getMiaoshaGoodsStock, "" + goodsId);
        if (stock < 0) {
            localOverMap.put(goodsId, true);
            return Result.error(CodeMsg.MIAO_SHA_OVER);
        }
        //判断是否已经秒杀到了,如果该用户已经秒杀过了，那么对redis做补偿
        MiaoshaOrder order = orderService.getMiaoshaOrderByUserIdGoodsId(user.getId(), goodsId);
        if (order != null) {
            stock = redisService.incr(GoodsKey.getMiaoshaGoodsStock,"" + goodsId);
            if (stock > 0) {
                localOverMap.put(goodsId,false);
            }
            return Result.error(CodeMsg.REPEATE_MIAOSHA);
        }
        // 异步操作，入mq队列,这里有2种情况待解决：1.入队失败：消息丢失;2.消费失败：数据库插入失败
        // 解决消息丢失：confirm机制；解决插入失败：ack机制
        MiaoshaMessage mm = new MiaoshaMessage();
        mm.setUser(user);
        mm.setGoodsId(goodsId);
        sender.sendMiaoshaMessage(mm);
        return Result.success(0); // 排队中
    }

    //第一版：通过Redis接口限流
    /*@RequestMapping(value = "/path", method = RequestMethod.GET)
    @ResponseBody
    public Result<String> getMiaoshaPath(HttpServletResponse response, HttpServletRequest request, @RequestParam("goodsId") long goodsId,
                                         @CookieValue(value = MiaoshaUserService.COOKI_NAME_TOKEN,required = false) String cookieToken,
                                         @RequestParam(value = MiaoshaUserService.COOKI_NAME_TOKEN,required = false) String paramToken,
                                         @RequestParam(value = "verifyCode", defaultValue = "0") int verifyCode) {
        if (StringUtils.isEmpty(cookieToken) && StringUtils.isEmpty(paramToken)) {
            return Result.error(CodeMsg.SESSION_ERROR);//token不存在或失效
        }
        String token = StringUtils.isEmpty(paramToken) ? cookieToken : paramToken;
        MiaoshaUser user = userService.getByToken(response, token);//从token中读用户信息

        //查询访问的次数（5秒中访问不超过5次）
        String uri = request.getRequestURI();
        String key = uri + "_" + user.getId();
        Integer count = redisService.get(AccessKey.access,key,Integer.class);
        if (count == null){
            redisService.set(AccessKey.access,key,1); //时间设的5秒
        }else if (count < 5){ //这样的设置是指5秒内访问5次是正常，否则返回失败
            redisService.incr(AccessKey.access,key);
        }else {
            return Result.error(CodeMsg.ACCESS_LIMIT_REACHED);
        }


        //验证码校验
        boolean check = miaoshaService.checkVerifyCode(user, goodsId, verifyCode);
        if (!check) {
            return Result.error(CodeMsg.REQUEST_ILLEGAL);
        }
        String path = miaoshaService.createMiaoshaPath(user, goodsId);
        return Result.success(path);
    }*/

    //第二版：通过注解限流
    @AccessLimit(seconds = 5, maxCount = 5, needLogin = true)
    @RequestMapping(value = "/path", method = RequestMethod.GET)
    @ResponseBody
    public Result<String> getMiaoshaPath(HttpServletResponse response, @RequestParam("goodsId") long goodsId,
                                         @CookieValue(value = MiaoshaUserService.COOKI_NAME_TOKEN,required = false) String cookieToken,
                                         @RequestParam(value = MiaoshaUserService.COOKI_NAME_TOKEN,required = false) String paramToken,
                                         @RequestParam(value = "verifyCode", defaultValue = "0") int verifyCode) {
        if (StringUtils.isEmpty(cookieToken) && StringUtils.isEmpty(paramToken)) {
            return Result.error(CodeMsg.SESSION_ERROR);//token不存在或失效
        }
        String token = StringUtils.isEmpty(paramToken) ? cookieToken : paramToken;
        MiaoshaUser user = userService.getByToken(response, token);//从token中读用户信息
        if (user == null) {
            return Result.error(CodeMsg.User_Not_Login);
        }
        //验证码校验
        boolean check = miaoshaService.checkVerifyCode(user, goodsId, verifyCode);
        if (!check) {
            return Result.error(CodeMsg.REQUEST_ILLEGAL);
        }
        String path = miaoshaService.createMiaoshaPath(user, goodsId);
        return Result.success(path);
    }

    @RequestMapping(value = "/verifyCode", method = RequestMethod.GET)
    @ResponseBody
    public Result<String> getMiaoshaVerifyCod(HttpServletResponse response,@RequestParam("goodsId") long goodsId,
                                              @CookieValue(value = MiaoshaUserService.COOKI_NAME_TOKEN,required = false) String cookieToken,
                                              @RequestParam(value = MiaoshaUserService.COOKI_NAME_TOKEN,required = false) String paramToken) {
        if (StringUtils.isEmpty(cookieToken) && StringUtils.isEmpty(paramToken)) {
            return Result.error(CodeMsg.SESSION_ERROR);//token不存在或失效
        }
        String token = StringUtils.isEmpty(paramToken) ? cookieToken : paramToken;
        MiaoshaUser user = userService.getByToken(response, token);//从token中读用户信息
        if (user == null) {
            return Result.error(CodeMsg.User_Not_Login);
        }
        try {
            BufferedImage image = miaoshaService.createVerifyCode(user, goodsId);
            OutputStream out = response.getOutputStream();
            ImageIO.write(image, "JPEG", out);
            out.flush();
            out.close();
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error(CodeMsg.MIAOSHA_FAIL);
        }
    }

    /**
     * orderId：成功
     * -1：秒杀失败
     * 0： 排队中
     */
    @RequestMapping(value = "/result", method = RequestMethod.GET)
    @ResponseBody
    public Result<Long> miaoshaResult(@RequestParam("goodsId") long goodsId,HttpServletResponse response,
                                      @CookieValue(value = MiaoshaUserService.COOKI_NAME_TOKEN,required = false) String cookieToken,
                                      @RequestParam(value = MiaoshaUserService.COOKI_NAME_TOKEN,required = false) String paramToken) {
        if (StringUtils.isEmpty(cookieToken) && StringUtils.isEmpty(paramToken)) {
            return Result.error(CodeMsg.SESSION_ERROR);//token不存在或失效
        }
        String token = StringUtils.isEmpty(paramToken) ? cookieToken : paramToken;
        MiaoshaUser user = userService.getByToken(response, token);//从token中读用户信息

        long result = miaoshaService.getMiaoshaResult(user.getId(), goodsId);
        return Result.success(result);
    }

    @RequestMapping(value = "/reset", method = RequestMethod.GET)
    @ResponseBody
    public Result<Boolean> reset() {
        List<GoodsVo> goodsList = goodsService.listGoodsVo();
        for (GoodsVo goods : goodsList) {
            goods.setStockCount(9);
            // 更新缓存，把商品库存还原
            redisService.set(GoodsKey.getMiaoshaGoodsStock, "" + goods.getId(), 9);
            localOverMap.put(goods.getId(), false);
        }
        redisService.delete(OrderKey.getMiaoshaOrderByUidGid);
        redisService.delete(MiaoshaKey.isGoodsOver);
        // 更新数据库：把商品库存还原，订单删除
        miaoshaService.reset(goodsList);
        return Result.success(true);
    }

}
