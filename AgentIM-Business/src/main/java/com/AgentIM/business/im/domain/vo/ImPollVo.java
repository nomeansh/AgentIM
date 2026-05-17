package com.AgentIM.business.im.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import com.AgentIM.business.im.domain.entity.ImPoll;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 投票视图对象。
 */
@Data
@AutoMapper(target = ImPoll.class)
public class ImPollVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long messageId;
    private String question;
    private String multiple;
    private String anonymous;
    private String status;
    private Date closeTime;
    private List<ImPollOptionVo> options;
}
