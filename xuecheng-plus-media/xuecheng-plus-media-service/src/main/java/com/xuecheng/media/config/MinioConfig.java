package com.xuecheng.media.config;

import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @description minio配置
 * @author Mr.M
 * @date 2022/9/12 19:32
 * @version 1.0
 */
//在这加上@Configuration注解下面的@Value注解才能够起作用
@Configuration
public class MinioConfig {


    //通过@Value注解获取nacos配置文件中的信息
    @Value("${minio.endpoint}")
    private String endpoint;
    @Value("${minio.accessKey}")
    private String accessKey;
    @Value("${minio.secretKey}")
    private String secretKey;

    //@Bean注解把minioClient这个对象放到spring容器中
    @Bean
    public MinioClient minioClient() {

        //这个操作就相当于获取与minio服务器的连接
        MinioClient minioClient =
                MinioClient.builder()
                        .endpoint(endpoint)
                        .credentials(accessKey, secretKey)
                        .build();
        return minioClient;
    }
}
