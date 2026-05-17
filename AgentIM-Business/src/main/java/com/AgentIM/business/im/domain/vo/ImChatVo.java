package com.AgentIM.business.im.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import com.AgentIM.business.im.domain.entity.ImChat;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 聊天会话视图对象。
 */
@Data
@AutoMapper(target = ImChat.class)
public class ImChatVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private String type;
    private String title;
    private String avatar;
    private String description;
    private Long ownerId;
    private Long seq;
    private Long lastMsgId;
    private String lastMsgContent;
    private Date lastMsgTime;
    private String status;
    private Long unreadCount;
    private List<ImChatMemberVo> members;
}
