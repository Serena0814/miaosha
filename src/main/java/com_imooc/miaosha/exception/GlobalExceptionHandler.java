package com_imooc.miaosha.exception;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import com_imooc.miaosha.result.CodeMsg;
import com_imooc.miaosha.result.Result;
import org.springframework.validation.BindException;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;



@ControllerAdvice      // @ControllerAdvice注解实现全局异常处理
@ResponseBody
public class GlobalExceptionHandler {

    @ExceptionHandler(value = Exception.class)    // 拦截什么类型的异常，这里是拦截所有异常
    public Result<String> exceptionHandler(HttpServletRequest request, Exception e) {
        e.printStackTrace();
        if (e instanceof GlobalException) {
            GlobalException ex = (GlobalException) e;
            return Result.error(ex.getCm());
        } else if (e instanceof BindException) {    // 参数校验异常
            BindException ex = (BindException) e;
            List<ObjectError> errors = ex.getAllErrors();
            ObjectError error = errors.get(0);
            String msg = error.getDefaultMessage(); // DefaultMessage是自定义注解@isMobile的默认message
            return Result.error(CodeMsg.BIND_ERROR.fillArgs(msg));
        } else {
            return Result.error(CodeMsg.SERVER_ERROR);
        }
    }
}
