package com_imooc.miaosha.vo;

import javax.validation.constraints.NotNull;

import com_imooc.miaosha.validator.IsMobile;
import org.hibernate.validator.constraints.Length;


public class LoginVo {

    @NotNull//表示不能为空
    @IsMobile//自定义一个校验器
    private String mobile;

    @NotNull
    @Length(min = 32)//表示长度最小为32位
    private String password;

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public String toString() {
        return "LoginVo [mobile=" + mobile + ", password=" + password + "]";
    }
}
