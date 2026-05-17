package com.AgentIM.business.im.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import com.AgentIM.business.im.domain.entity.ImMessage;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 消息视图对象。
 */
@Data
@AutoMapper(target = ImMessage.class)
public class ImMessageVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long chatId;
    private Long senderId;
    private String senderUsername;
    private String senderNickname;
    private String messageType;
    private String content;
    private String contentPayload;
    private String resourceIds;
    private Long replyToMessageId;
    private Long forwardFromMessageId;
    private Long forwardFromChatId;
    private Long forwardSenderId;
    private String clientMsgId;
    private String idempotentKey;
    private Long seq;
    private Integer replyCount;
    private String status;
    private Date editTime;
    private Date deleteTime;
    private Date createTime;
    private List<ImReactionSummaryVo> reactions;
}
