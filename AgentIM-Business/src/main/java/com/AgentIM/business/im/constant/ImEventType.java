package com.AgentIM.business.im.constant;

/**
 * P0 WebSocket 领域事件类型。
 *
 * <p>事件类型采用文档约定的点分命名。Service 层只引用这些常量，避免事件生产者与前端、
 * WebSocket 消费者之间因为硬编码字符串不一致而产生兼容问题。</p>
 */
public final class ImEventType {

    public static final String MESSAGE_CREATED = "message.created";
    public static final String MESSAGE_EDITED = "message.edited";
    public static final String MESSAGE_DELETED = "message.deleted";
    public static final String MESSAGE_HIDDEN = "message.hidden";
    public static final String MESSAGE_PINNED = "message.pinned";
    public static final String MESSAGE_UNPINNED = "message.unpinned";
    public static final String REACTION_CREATED = "reaction.created";
    public static final String REACTION_DELETED = "reaction.deleted";
    public static final String READ_STATE_UPDATED = "read_state.updated";
    public static final String CHAT_CREATED = "chat.created";
    public static final String CHAT_UPDATED = "chat.updated";
    public static final String CHAT_DELETED = "chat.deleted";
    public static final String USER_STATUS = "user.status";
    public static final String TYPING = "typing";

    /**
     * 阻止实例化事件类型常量类。
     *
     * <p>事件类型没有运行时状态，保持私有构造器可以防止误用为可注入 Bean 或普通对象。</p>
     */
    private ImEventType() {
    }
}
