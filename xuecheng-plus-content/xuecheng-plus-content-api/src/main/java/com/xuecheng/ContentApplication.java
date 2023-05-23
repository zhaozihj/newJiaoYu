package com.xuecheng;

import com.spring4all.swagger.EnableSwagger2Doc;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

/*
内容管理服务启动类，放到xuecheng目录下可以扫描到com.xuecheng包下以及自包下所有的bean
 */
@SpringBootApplication
@EnableSwagger2Doc
public class ContentApplication {

    public static void main(String[] args) {
        SpringApplication.run(ContentApplication.class,args);
    }
}
