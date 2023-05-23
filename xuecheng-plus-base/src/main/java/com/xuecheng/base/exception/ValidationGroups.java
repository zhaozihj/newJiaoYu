package com.xuecheng.base.exception;

/**
 * 用于分组校验，定义一些常用的组
 */
public class ValidationGroups {


    //这里是定义了Inster，Update，Delete三个组，分别用来存放三种(insert,update,delete)操作时对应的标记，也就是存放JSR303校验的注解以及注解的message信息
public interface Inster{};
public interface Update{};
public interface Delete{};


}
