package com.AgentIM.business.im.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 联系人列表视图对象。
 */
@Data
public class ImContactVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long contactUserId;
    private String username;
    private String nickname;
    private String avatar;
    private String remark;
    private String status;
}
