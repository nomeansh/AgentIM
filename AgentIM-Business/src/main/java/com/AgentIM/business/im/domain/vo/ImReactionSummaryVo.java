package com.AgentIM.business.im.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 消息反应聚合视图对象。
 */
@Data
public class ImReactionSummaryVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String reaction;
    private Long count;
}
