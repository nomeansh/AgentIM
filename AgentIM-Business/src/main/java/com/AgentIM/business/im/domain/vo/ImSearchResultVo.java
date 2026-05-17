package com.AgentIM.business.im.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * IM 搜索结果视图对象。
 */
@Data
public class ImSearchResultVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private List<ImMessageVo> messages;
    private List<ImUserVo> users;
}
