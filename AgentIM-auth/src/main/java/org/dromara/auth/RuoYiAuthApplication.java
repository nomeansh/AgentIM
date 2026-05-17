package org.dromara.auth;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.context.metrics.buffering.BufferingApplicationStartup;

/**
 * 认证授权中心
 *
 * @author ruoyi
 */
@EnableDubbo
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
public class RuoYiAuthApplication {

    /**
     * 启动 AgentIM 认证授权中心。
     *
     * <p>Auth 服务负责验证码、登录、注册入口和 Sa-Token 令牌签发，不直接连接业务数据库，因此
     * 启动时排除 {@link DataSourceAutoConfiguration}。P0 核心 IM 的账号资料创建和密码校验
     * 通过 Dubbo 调用 Business 服务完成，认证中心只保留认证流程编排职责。</p>
     *
     * @param args JVM 启动参数，透传给 Spring Boot 以支持 profile、Nacos 地址等运行时配置
     */
    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(RuoYiAuthApplication.class);
        application.setApplicationStartup(new BufferingApplicationStartup(2048));
        application.run(args);
        System.out.println("(♥◠‿◠)ﾉﾞ  认证授权中心启动成功   ლ(´ڡ`ლ)ﾞ  ");
    }
}
