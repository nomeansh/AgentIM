package com.AgentIM.business.im.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 消息已读成员视图对象。
 */
@Data
public class ImReadMemberVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long userId;
    private String username;
    private String nickname;
    private String avatar;
    private Long lastReadMessageId;
    private Long lastReadSeq;
    private Date readTime;
    private Boolean hasRead;
}
