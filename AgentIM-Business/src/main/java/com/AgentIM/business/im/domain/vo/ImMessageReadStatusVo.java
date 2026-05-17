package com.AgentIM.business.im.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 消息已读状态视图对象。
 */
@Data
public class ImMessageReadStatusVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String status;
    private Integer totalCount;
    private Integer readCount;
    private Double ratio;
    private List<ImReadMemberVo> readBy;
    private List<ImReadMemberVo> unreadBy;
}
