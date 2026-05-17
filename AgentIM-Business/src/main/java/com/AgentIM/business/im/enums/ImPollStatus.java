package com.AgentIM.business.im.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 投票状态。
 *
 * <p>P0 投票随消息创建，active 表示仍可投票，closed 表示已经手动或按 closeTime 关闭。</p>
 */
@Getter
@AllArgsConstructor
public enum ImPollStatus implements ImCodeEnum {

    ACTIVE("active", "进行中"),
    CLOSED("closed", "已结束");

    private final String code;
    private final String desc;
}
