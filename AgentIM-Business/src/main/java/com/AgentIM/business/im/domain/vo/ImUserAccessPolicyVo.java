package com.AgentIM.business.im.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 用户级数据访问策略视图对象。
 */
@Data
public class ImUserAccessPolicyVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private String username;
    private String nickname;
    private String status;
    private String dataScope;
    private String permissionTags;
    private String accessPolicy;
    private Date updateTime;
}
