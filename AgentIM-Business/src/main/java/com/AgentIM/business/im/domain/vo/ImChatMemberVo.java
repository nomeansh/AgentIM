package com.AgentIM.business.im.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import com.AgentIM.business.im.domain.entity.ImChatMember;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 聊天成员视图对象。
 */
@Data
@AutoMapper(target = ImChatMember.class)
public class ImChatMemberVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long chatId;
    private Long userId;
    private String username;
    private String nickname;
    private String avatar;
    private String role;
    private Date joinedTime;
    private String muted;
}
