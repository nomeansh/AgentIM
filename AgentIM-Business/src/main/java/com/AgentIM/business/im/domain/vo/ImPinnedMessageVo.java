package com.AgentIM.business.im.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import com.AgentIM.business.im.domain.entity.ImPinnedMessage;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 置顶消息视图对象。
 */
@Data
@AutoMapper(target = ImPinnedMessage.class)
public class ImPinnedMessageVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long chatId;
    private Long messageId;
    private Long pinnedBy;
    private Date pinnedTime;
    private ImMessageVo message;
}
