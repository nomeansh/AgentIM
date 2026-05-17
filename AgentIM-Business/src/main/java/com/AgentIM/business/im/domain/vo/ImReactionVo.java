package com.AgentIM.business.im.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import com.AgentIM.business.im.domain.entity.ImMessageReaction;

import java.io.Serial;
import java.io.Serializable;

/**
 * 消息反应明细视图对象。
 */
@Data
@AutoMapper(target = ImMessageReaction.class)
public class ImReactionVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long messageId;
    private Long userId;
    private String username;
    private String nickname;
    private String reaction;
}
