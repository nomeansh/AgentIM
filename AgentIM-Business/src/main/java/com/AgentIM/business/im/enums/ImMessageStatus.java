package com.AgentIM.business.im.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 消息状态。
 *
 * <p>消息删除采用逻辑状态表达。对所有人删除会将状态改为 deleted_all，而仅自己删除记录在
 * im_user_message_hide 中，不修改主消息状态。</p>
 */
@Getter
@AllArgsConstructor
public enum ImMessageStatus implements ImCodeEnum {

    NORMAL("normal", "正常"),
    EDITED("edited", "已编辑"),
    DELETED_ALL("deleted_all", "对所有人删除");

    private final String code;
    private final String desc;
}
