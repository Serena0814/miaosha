package com_imooc.miaosha.validator;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import com_imooc.miaosha.util.ValidatorUtil;
import org.apache.commons.lang3.StringUtils;

public class IsMobileValidator implements ConstraintValidator<IsMobile, String> {

    // required表示是否需要验证
    private boolean required = false;

    // 初始化方法：自定义注解IsMobile中的required()方法的返回值赋给require
    // 即取出用户使用自定义注解时设置的required()
    public void initialize(IsMobile constraintAnnotation) {
        required = constraintAnnotation.required();
    }

    // 该方法判断被IsMobile注解标记的字段是否合法
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // 如果required为true则需要验证则验证手机号
        if (required) {
            return ValidatorUtil.isMobile(value);
        } else {
            if (StringUtils.isEmpty(value)) {
                return true;
            } else {
                return ValidatorUtil.isMobile(value);
            }
        }
    }

}
