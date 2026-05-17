package com.AgentIM.business.im.domain.bo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 聊天成员变更入参。
 *
 * <p>群组可指定 member 或 admin，频道会在服务层将普通用户标准化为 subscriber，私聊不允许通过
 * 成员管理接口变更成员。</p>
 */
@Data
public class ImChatMemberBo {

    /**
     * 被添加或调整角色的用户 ID。
     */
    @NotNull(message = "成员用户ID不能为空")
    private Long userId;

    /**
     * 目标角色。
     */
    private String role;

    /**
     * 是否免打扰，0 表示正常，1 表示免打扰。
     */
    private String muted;
}
