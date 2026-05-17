package com.AgentIM.business.im.domain.bo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 发送消息入参。
 *
 * <p>该对象覆盖文本、媒体、投票、引用回复和转发消息。媒体消息必须先上传资源，再在 resourceIds
 * 中传入资源 ID；幂等键用于客户端重试防重复。</p>
 */
@Data
public class ImMessageSendBo {

    /**
     * 消息类型。
     */
    @NotBlank(message = "消息类型不能为空")
    private String messageType;

    /**
     * 消息正文或摘要。
     */
    @Size(max = 4096, message = "消息正文长度不能超过 4096")
    private String content;

    /**
     * JSON 字符串结构化载荷，例如投票选项。
     */
    private String contentPayload;

    /**
     * 引用的资源 ID 列表。
     */
    private List<Long> resourceIds;

    /**
     * 被回复消息 ID。
     */
    private Long replyToMessageId;

    /**
     * 转发来源消息 ID。
     */
    private Long forwardFromMessageId;

    /**
     * 转发来源聊天 ID。
     */
    private Long forwardFromChatId;

    /**
     * 客户端消息 ID。
     */
    @Size(max = 100, message = "客户端消息ID长度不能超过 100")
    private String clientMsgId;

    /**
     * 幂等键。
     */
    @NotBlank(message = "幂等键不能为空")
    @Size(max = 120, message = "幂等键长度不能超过 120")
    private String idempotentKey;
}
