package com_imooc.miaosha.redis;

public class GoodsKey extends BasePrefix {

    private GoodsKey(int expireSeconds, String prefix) {
        super(expireSeconds, prefix);
    }

    // 页面缓存的有效期会比较短，60s
    // 因为页面发生变化但缓存中不发生变化，所以用户看到60s之前的页面问题不大
    public static GoodsKey getGoodsList = new GoodsKey(60, "gl");
    public static GoodsKey getGoodsDetail = new GoodsKey(60, "gd");
    public static GoodsKey getMiaoshaGoodsStock = new GoodsKey(0, "gs"); // 0为永久存在
}
