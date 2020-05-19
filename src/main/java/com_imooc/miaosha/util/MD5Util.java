package com_imooc.miaosha.util;

import org.apache.commons.codec.digest.DigestUtils;

public class MD5Util {

    public static String md5(String src) {
        return DigestUtils.md5Hex(src);
    }

    private static final String salt = "1a2b3c4d";

    // 从客户输入到网上流传，第一次md5
    public static String inputPassToFormPass(String inputPass) {
        String str = "" + salt.charAt(0) + salt.charAt(2) + inputPass + salt.charAt(5) + salt.charAt(4);
       // System.out.println(str);
        return md5(str);
    }

    // 从网上流传到数据库，第二次md5
    public static String formPassToDBPass(String formPass, String salt) {
        String str = "" + salt.charAt(0) + salt.charAt(2) + formPass + salt.charAt(5) + salt.charAt(4);
        return md5(str);
    }

    // saltDB是存储在数据库端的salt
    public static String inputPassToDbPass(String inputPass, String saltDB) {
        String formPass = inputPassToFormPass(inputPass);
        String dbPass = formPassToDBPass(formPass, saltDB);
        return dbPass;
    }

    public static void main(String[] args) {
        System.out.println(inputPassToFormPass("12222222222"));
//		System.out.println(formPassToDBPass(inputPassToFormPass("123456"), "1a2b3c4d"));
		System.out.println(inputPassToDbPass("12222222222", "123456"));
    }

}
