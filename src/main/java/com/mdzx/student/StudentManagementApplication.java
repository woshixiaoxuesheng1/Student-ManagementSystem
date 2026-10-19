package com.mdzx.student;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


/**
 * 应用启动入口，负责装配并运行 Spring Boot 服务。
 */
/* @SpringBootApplication包含以下三个重要注解
* @SpringBootConfiguration 表示该类是spring boot的配置类
* @EnableAutoConfiguration 开启spring boot自动配置
* @ComponentScan 扫描启动类所在包及其子包 并将其注册到IOC容器中
* */
@SpringBootApplication
//如果启动类使用该注解 那么通常就不必再给这个包中的每个 Mapper 接口单独添加 @Mapper
@MapperScan("com.mdzx.student.mapper")
public class StudentManagementApplication {

    /** 启动 Spring Boot 应用。 */
    public static void main(String[] args) {
        SpringApplication.run(StudentManagementApplication.class, args);
    }
}
