package com.AgentIM.business.im.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import com.AgentIM.business.im.domain.entity.ImPollOption;

import java.io.Serial;
import java.io.Serializable;

/**
 * 投票选项视图对象。
 */
@Data
@AutoMapper(target = ImPollOption.class)
public class ImPollOptionVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long pollId;
    private String text;
    private Integer ordinal;
    private Long voteCount;
    private Boolean selectedByMe;
}
