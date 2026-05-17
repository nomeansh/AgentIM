package com.AgentIM.business.im.domain.bo;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 更新聊天设置入参。
 *
 * <p>该对象仅包含可由 owner/admin 修改的展示字段，不允许通过更新接口直接修改聊天类型、owner
 * 或消息序号。</p>
 */
@Data
public class ImChatUpdateBo {

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
}
