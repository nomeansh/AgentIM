package com.AgentIM.business.im.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * IM 事件发布配置。
 *
 * <p>该配置集中管理 P0 事件链路的运行方式。默认情况下不强制依赖 RocketMQ，事件会直接通过
 * WebSocket 分布式通道发布；当部署环境配置 RocketMQ 并开启开关后，事件先进入 RocketMQ，
 * 再由消费者转发到 WebSocket，便于后续扩展重试、削峰和跨服务消费。</p>
 */
@Data
@Component
@ConfigurationProperties(prefix = "agentim.im.event")
public class ImEventProperties {

    /**
     * RocketMQ 发送失败时是否回退为 WebSocket 直发。
     */
    private boolean websocketFallback = true;

    /**
     * RocketMQ 事件配置。
     */
    private RocketMq rocketmq = new RocketMq();

    /**
     * 构建 RocketMQ 发送目的地。
     *
     * <p>RocketMQTemplate 使用 {@code topic:tag} 作为目的地格式。tag 为空时只返回 topic，
     * 便于部署环境只按 topic 订阅。</p>
     *
     * @return RocketMQTemplate 可识别的目的地字符串
     */
    public String rocketMqDestination() {
        if (rocketmq.getTag() == null || rocketmq.getTag().isBlank()) {
            return rocketmq.getTopic();
        }
        return rocketmq.getTopic() + ":" + rocketmq.getTag();
    }

    /**
     * RocketMQ 事件通道配置。
     *
     * <p>该内部配置类只保存事件 topic、tag 和开关，真正的 name-server、producer group 等
     * RocketMQ 原生配置仍然走框架标准的 {@code rocketmq.*} 配置项。</p>
     */
    @Data
    public static class RocketMq {

        /**
         * 是否启用 RocketMQ 事件链路。
         */
        private boolean enabled = false;

        /**
         * IM 事件 Topic。
         */
        private String topic = "agentim-im-event";

        /**
         * IM 事件 Tag。
         */
        private String tag = "im";
    }
}
