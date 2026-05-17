package com.AgentIM.business.im.support;

import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.dromara.common.websocket.dto.WebSocketMessageDto;
import org.dromara.common.websocket.utils.WebSocketUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * IM RocketMQ 事件消费者。
 *
 * <p>当 {@code agentim.im.event.rocketmq.enabled=true} 时，Business 服务会把事务提交后的
 * IM 事件发送到 RocketMQ。该消费者订阅同一 Topic，并把消息转发到现有 WebSocket 分布式通道，
 * 从而保留多设备实时推送能力，同时为后续可靠事件投递和异步消费打基础。</p>
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "agentim.im.event.rocketmq", name = "enabled", havingValue = "true")
@RocketMQMessageListener(
    topic = "${agentim.im.event.rocketmq.topic:agentim-im-event}",
    selectorExpression = "${agentim.im.event.rocketmq.tag:im}",
    consumerGroup = "${spring.application.name:agentim-business}-im-event-consumer"
)
public class ImRocketMqEventConsumer implements RocketMQListener<WebSocketMessageDto> {

    /**
     * 消费 RocketMQ 中的 IM 事件并转发到 WebSocket。
     *
     * <p>生产者已经把事件封装成 {@link WebSocketMessageDto}，因此消费者不再解析业务 payload，
     * 只负责校验目标会话集合并调用公共 WebSocket 工具发布。无目标用户的消息会被忽略，避免
     * 空广播造成无意义日志和网络开销。</p>
     *
     * @param message RocketMQ 反序列化后的 WebSocket 消息 DTO
     */
    @Override
    public void onMessage(WebSocketMessageDto message) {
        if (message == null || message.getSessionKeys() == null || message.getSessionKeys().isEmpty()) {
            log.warn("忽略无目标用户的 IM RocketMQ 事件");
            return;
        }
        WebSocketUtils.publishMessage(message);
    }
}
