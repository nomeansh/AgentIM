package com.AgentIM.business.im.support;

import com.AgentIM.business.im.config.ImEventProperties;
import com.AgentIM.business.im.mapper.ImChatMemberMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.dromara.common.json.utils.JsonUtils;
import org.dromara.common.websocket.dto.WebSocketMessageDto;
import org.dromara.common.websocket.utils.WebSocketUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * IM 领域事件发布器。
 *
 * <p>Service 层在事务内调用该组件注册事件，组件会在事务提交后发布给目标用户。默认路径是
 * WebSocket 分布式发布；当配置启用 RocketMQ 时，事件会先写入 RocketMQ，再由消费者转发到
 * WebSocket，避免事务内执行外部网络调用并保留后续可靠事件投递扩展点。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ImEventPublisher {

    private final ImChatMemberMapper chatMemberMapper;

    private final ImEventProperties eventProperties;

    private final ObjectProvider<RocketMQTemplate> rocketMQTemplateProvider;

    /**
     * 向聊天所有成员发布事件。
     *
     * <p>方法先查询当前聊天有效成员，再转交给 publishToUsers。事件发送会延迟到事务提交之后，
     * 避免客户端收到尚未提交的数据。</p>
     *
     * @param eventType 事件类型
     * @param chatId 聊天 ID
     * @param actorId 操作人 ID
     * @param payload 事件载荷
     */
    public void publishToChat(String eventType, Long chatId, Long actorId, Object payload) {
        List<Long> userIds = chatMemberMapper.selectUserIdsByChatId(chatId);
        publishToUsers(eventType, chatId, actorId, userIds, payload);
    }

    /**
     * 向指定用户集合发布事件。
     *
     * <p>该方法保留多设备语义：sessionKey 使用 userId，底层 WebSocket 工具会把消息投递给该用户
     * 在当前节点或其他节点上的所有活跃连接。</p>
     *
     * @param eventType 事件类型
     * @param chatId 关联聊天 ID，可为空
     * @param actorId 操作人 ID，可为空
     * @param userIds 目标用户 ID 集合
     * @param payload 事件载荷
     */
    public void publishToUsers(String eventType, Long chatId, Long actorId, Collection<Long> userIds, Object payload) {
        if (userIds == null || userIds.isEmpty()) {
            return;
        }
        WebSocketMessageDto dto = new WebSocketMessageDto();
        dto.setSessionKeys(userIds.stream().filter(item -> item != null).distinct().toList());
        dto.setMessage(JsonUtils.toJsonString(buildEvent(eventType, chatId, actorId, payload)));
        publishAfterCommit(dto);
    }

    /**
     * 在事务提交后发送 IM 事件。
     *
     * <p>当调用点存在 Spring 事务同步时注册 afterCommit；没有事务时立即发送，方便查询类或非事务
     * 操作复用同一发布入口。真正发送时会根据配置优先选择 RocketMQ 或 WebSocket 直发。</p>
     *
     * @param dto WebSocket 消息 DTO
     */
    private void publishAfterCommit(WebSocketMessageDto dto) {
        if (dto.getSessionKeys() == null || dto.getSessionKeys().isEmpty()) {
            return;
        }
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                /**
                 * 事务提交后投递事件。
                 *
                 * <p>只有数据库提交成功才会触发客户端更新，避免消息发送失败但客户端已经收到事件的
                 * 不一致状态。</p>
                 */
                @Override
                public void afterCommit() {
                    dispatch(dto);
                }
            });
            return;
        }
        dispatch(dto);
    }

    /**
     * 按配置分发事件。
     *
     * <p>RocketMQ 开启时，方法把事件写入配置的 topic/tag；未开启时直接调用 WebSocket 工具。
     * 如果 RocketMQ 发送失败且允许回退，则立即执行 WebSocket 直发，保证本次业务操作仍能触发
     * 实时通知。回退只用于实时体验，不代表可靠事件已经持久化。</p>
     *
     * @param dto WebSocket 消息 DTO
     */
    private void dispatch(WebSocketMessageDto dto) {
        if (eventProperties.getRocketmq().isEnabled()) {
            try {
                RocketMQTemplate rocketMQTemplate = rocketMQTemplateProvider.getIfAvailable();
                if (rocketMQTemplate == null) {
                    throw new IllegalStateException("RocketMQTemplate 未初始化");
                }
                rocketMQTemplate.convertAndSend(eventProperties.rocketMqDestination(), dto);
                return;
            } catch (Exception e) {
                log.error("IM 事件写入 RocketMQ 失败，destination={}", eventProperties.rocketMqDestination(), e);
                if (!eventProperties.isWebsocketFallback()) {
                    return;
                }
            }
        }
        WebSocketUtils.publishMessage(dto);
    }

    /**
     * 构建统一事件对象。
     *
     * <p>事件字段与 P0 文档保持一致：eventId、eventType、chatId、actorId、payload、
     * occurredAt。使用 LinkedHashMap 保持序列化字段顺序稳定，便于日志排查。</p>
     *
     * @param eventType 事件类型
     * @param chatId 关联聊天 ID
     * @param actorId 操作人 ID
     * @param payload 业务载荷
     * @return 可序列化事件 Map
     */
    private Map<String, Object> buildEvent(String eventType, Long chatId, Long actorId, Object payload) {
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("eventId", "evt_" + UUID.randomUUID().toString().replace("-", ""));
        event.put("eventType", eventType);
        event.put("chatId", chatId);
        event.put("actorId", actorId);
        event.put("payload", payload);
        event.put("occurredAt", new Date());
        return event;
    }
}
