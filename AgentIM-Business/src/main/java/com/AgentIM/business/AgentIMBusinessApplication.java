package com.AgentIM.business;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.metrics.buffering.BufferingApplicationStartup;

/**
 * AgentIM 业务服务启动入口。
 *
 * <p>该服务承载 P0 核心 IM 的 HTTP 接口、Dubbo 认证账号契约、MyBatis-Plus
 * 数据访问和事务后 WebSocket 事件发布能力。启动类显式扫描 IM Mapper，避免业务模块
 * 作为独立微服务运行时出现 Mapper 未注册的问题。</p>
 */
@EnableDubbo
@MapperScan("com.AgentIM.business.im.mapper")
@SpringBootApplication
public class AgentIMBusinessApplication {

    /**
     * 启动 AgentIM Business 微服务。
     *
     * <p>方法创建 Spring Boot 应用上下文并启用启动阶段指标缓冲，便于后续在 actuator
     * 或启动日志中分析慢启动组件。该入口不承担业务初始化，所有业务数据的创建都通过
     * Service 层事务完成，符合 P0 文档对事务边界的要求。</p>
     *
     * @param args JVM 启动参数，透传给 Spring Boot 以支持 profile、Nacos 地址等运行时配置
     */
    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(AgentIMBusinessApplication.class);
        application.setApplicationStartup(new BufferingApplicationStartup(2048));
        application.run(args);
        System.out.println("AgentIM 业务服务模块启动成功");
    }
}
