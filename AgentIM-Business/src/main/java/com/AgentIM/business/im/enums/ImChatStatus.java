package com.AgentIM.business.im.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 聊天状态。
 *
 * <p>聊天状态控制会话是否出现在主列表、是否可继续访问以及是否已被业务删除。P0 使用状态字段
 * 保留历史消息，不通过物理删除破坏消息留存。</p>
 */
@Getter
@AllArgsConstructor
public enum ImChatStatus implements ImCodeEnum {

    ACTIVE("active", "活跃"),
    ARCHIVED("archived", "已归档"),
    DELETED("deleted", "已删除");

    private final String code;
    private final String desc;
}
