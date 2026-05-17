package com.AgentIM.business.im.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import com.AgentIM.business.im.domain.entity.ImUser;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * IM 用户资料视图对象。
 */
@Data
@AutoMapper(target = ImUser.class)
public class ImUserVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private String username;
    private String nickname;
    private String avatar;
    private String bio;
    private String phone;
    private String email;
    private String status;
    private Date createTime;
}
