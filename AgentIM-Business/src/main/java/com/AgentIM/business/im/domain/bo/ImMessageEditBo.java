package com.AgentIM.business.im.domain.bo;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 编辑消息入参。
 *
 * <p>编辑只允许修改内容、结构化载荷和资源引用，不允许改变消息类型、发送者、聊天归属或消息序号。</p>
 */
@Data
public class ImMessageEditBo {

    /**
     * 更新后的正文。
     */
    @Size(max = 4096, message = "消息正文长度不能超过 4096")
    private String content;

    /**
     * 更新后的 JSON 结构化载荷。
     */
    private String contentPayload;

    /**
     * 更新后的资源 ID 列表。
     */
    private List<Long> resourceIds;
}
