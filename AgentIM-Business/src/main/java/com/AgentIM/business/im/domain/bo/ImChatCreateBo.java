package com.AgentIM.business.im.domain.bo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 创建聊天入参。
 *
 * <p>该对象统一承载私聊、群组和频道创建请求。保存的消息由注册流程自动创建，不允许普通接口
 * 重复创建。</p>
 */
@Data
public class ImChatCreateBo {

    /**
     * 聊天类型：private、group、channel。
     */
    @NotBlank(message = "聊天类型不能为空")
    private String type;

    /**
     * 群组或频道标题。
     */
    @Size(max = 100, message = "聊天标题长度不能超过 100")
    private String title;

    /**
     * 群组或频道头像。
     */
    @Size(max = 500, message = "聊天头像长度不能超过 500")
    private String avatar;

    /**
     * 群组或频道简介。
     */
    @Size(max = 500, message = "聊天简介长度不能超过 500")
    private String description;

    /**
     * 初始成员 ID 列表。私聊必须且只能传入对方一个用户 ID。
     */
    private List<Long> memberIds;
}
