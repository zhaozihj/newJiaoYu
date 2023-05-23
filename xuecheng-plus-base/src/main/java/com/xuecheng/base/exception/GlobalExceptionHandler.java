package com.xuecheng.base.exception;


import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@ControllerAdvice
//@RestControllerAdvice    相当于@ControllerAdvice加上@ResponseBody
//异常处理器
public class GlobalExceptionHandler {


    //对项目的自定义异常类型进行处理
    //返回给前端的是json数据
    @ResponseBody
    //指定这个方法能够捕获和处理的异常
    @ExceptionHandler(XueChengPlusException.class)
    //决定相应给前端的状态码，也就是这个注解的value值决定
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public RestErrorResponse customException(XueChengPlusException e)
    {
        //记录异常
        log.error("系统异常{}",e.getErrMessage(),e);


        //解析出异常信息
        String errMessage = e.getErrMessage();
        RestErrorResponse restErrorResponse = new RestErrorResponse(errMessage);
        return restErrorResponse;
    }


     //不是系统自定义的异常，会被这个方法给捕获
    //返回给前端的是json数据
    @ResponseBody
    //指定这个方法能够捕获和处理的异常
    @ExceptionHandler(Exception.class)
    //决定相应给前端的状态码，也就是这个注解的value值决定
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public RestErrorResponse exception(Exception e)
    {
        //记录异常
        log.error("系统异常{}",e.getMessage(),e);


        //解析出异常信息
        //返回一个统一的异常信息 CommonError.UNKOWN_ERROR对应的  执行过程异常，请重试。
        RestErrorResponse restErrorResponse = new RestErrorResponse(CommonError.UNKOWN_ERROR.getErrMessage());
        return restErrorResponse;
    }


    //捕获MethodArgumentNotValidException异常，是JSR303校验成功之后抛出的异常
    //返回给前端的是json数据
    @ResponseBody
    //指定这个方法能够捕获和处理的异常
    @ExceptionHandler(MethodArgumentNotValidException.class)
    //决定相应给前端的状态码，也就是这个注解的value值决定
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public RestErrorResponse methodArgumentNotValidException(MethodArgumentNotValidException e)
    {

        BindingResult bindingResult=e.getBindingResult();
        //存储信息
        List<String> errors=new ArrayList<>();

        //获取JSR303校验返回的所有错误信息，可以同时返回多个  ，就是返回@NotEmpty(message = "课程名称不能为空")注解中message属性中的值
        //如果有多个JSR303校验注解 校验成功，则可以返回多个message
        bindingResult.getFieldErrors().stream().forEach(item->{
            errors.add(item.getDefaultMessage());
        });

        //将list中的错误信息拼接起来
        String errMessage= StringUtils.join(errors,",");

        //记录异常
        log.error("系统异常{}",e.getMessage(),errMessage);


        //解析出异常信息
        //返回一个统一的异常信息 CommonError.UNKOWN_ERROR对应的  执行过程异常，请重试。
        RestErrorResponse restErrorResponse = new RestErrorResponse(errMessage);
        return restErrorResponse;
    }


}
