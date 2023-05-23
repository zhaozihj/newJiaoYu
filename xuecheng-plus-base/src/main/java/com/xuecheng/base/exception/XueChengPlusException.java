package com.xuecheng.base.exception;


import com.xuecheng.base.exception.CommonError;

/**
 * @description 学成在线项目异常类,本项目自定义异常类型
 * @author Mr.M
 * @date 2022/9/6 11:29
 * @version 1.0
 */
public class XueChengPlusException extends RuntimeException {

    private String errMessage;

    public void setErrMessage(String errMessage) {
        this.errMessage = errMessage;
    }

    public XueChengPlusException() {
        super();
    }

    public XueChengPlusException(String errMessage) {
        super(errMessage);
        this.errMessage = errMessage;
    }

    public String getErrMessage() {
        return errMessage;
    }

    public static void cast(CommonError commonError){
        throw new XueChengPlusException(commonError.getErrMessage());
    }
    public static void cast(String errMessage){
        throw new XueChengPlusException(errMessage);
    }

}
