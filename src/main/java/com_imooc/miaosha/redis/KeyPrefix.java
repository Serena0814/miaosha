package com_imooc.miaosha.redis;

public interface KeyPrefix {

    public int expireSeconds();//过期时间

    public String getPrefix();

}
