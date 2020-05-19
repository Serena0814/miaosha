package com_imooc.miaosha.service;

import java.util.Date;
import com_imooc.miaosha.dao.OrderDao;
import com_imooc.miaosha.domain.MiaoshaOrder;
import com_imooc.miaosha.domain.MiaoshaUser;
import com_imooc.miaosha.domain.OrderInfo;
import com_imooc.miaosha.redis.OrderKey;
import com_imooc.miaosha.redis.RedisService;
import com_imooc.miaosha.vo.GoodsVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;



@Service
public class OrderService {

    @Autowired
    OrderDao orderDao;

    @Autowired
    RedisService redisService;

    public MiaoshaOrder getMiaoshaOrderByUserIdGoodsId(long userId, long goodsId) {
       // return orderDao.getMiaoshaOrderByUserIdGoodsId(userId, goodsId);
        //在缓存中查
        return redisService.get(OrderKey.getMiaoshaOrderByUidGid, "" + userId + "_" + goodsId, MiaoshaOrder.class);
    }

    @Transactional
    public OrderInfo createOrder(MiaoshaUser user, GoodsVo goods) {
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setCreateDate(new Date());
        orderInfo.setDeliveryAddrId(0L);
        orderInfo.setGoodsCount(1);
        orderInfo.setGoodsId(goods.getId());
        orderInfo.setGoodsName(goods.getGoodsName());
        orderInfo.setGoodsPrice(goods.getMiaoshaPrice());
        orderInfo.setOrderChannel(1);
        orderInfo.setStatus(0);
        orderInfo.setUserId(user.getId());
        orderDao.insert(orderInfo);
        MiaoshaOrder miaoshaOrder = new MiaoshaOrder();
        miaoshaOrder.setGoodsId(goods.getId());
        miaoshaOrder.setOrderId(orderInfo.getId());
        miaoshaOrder.setUserId(user.getId());
        miaoshaOrder.setuUidGid(user.getId()+ ""+goods.getId());
        orderDao.insertMiaoshaOrder(miaoshaOrder);
        //放入缓存中
        redisService.set(OrderKey.getMiaoshaOrderByUidGid, "" + user.getId() + "_" + goods.getId(), miaoshaOrder);
        return orderInfo;
    }

    public OrderInfo getOrderById(long orderId) {
        return orderDao.getOrderById(orderId);
    }

    public void clearOrders() {
        orderDao.clearOrders();
        orderDao.clearMiaoshaOrders();
    }
}