package com_imooc.miaosha.validator;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

@Target({METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER})
@Retention(RUNTIME)
@Documented
@Constraint(validatedBy = {IsMobileValidator.class}) //系统会调用这个校验器进行校验
public @interface IsMobile {

    boolean required() default true;//默认参数必须有值（不能为控）

    String message() default "手机号码格式错误";//如果验证不通过的信息

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
