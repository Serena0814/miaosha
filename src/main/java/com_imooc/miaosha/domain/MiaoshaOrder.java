package com_imooc.miaosha.domain;

public class MiaoshaOrder {
    private Long id;
    private Long userId;
    private Long orderId;
    private Long goodsId;
    private String uUidGid;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public Long getGoodsId() {
        return goodsId;
    }

    public void setGoodsId(Long goodsId) {
        this.goodsId = goodsId;
    }

    public String getuUidGid() {
        return uUidGid;
    }

    public void setuUidGid(String uUidGid) {
        this.uUidGid = uUidGid;
    }
}
