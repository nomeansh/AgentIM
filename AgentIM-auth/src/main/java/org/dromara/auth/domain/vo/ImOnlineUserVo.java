package org.dromara.auth.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 在线用户快照，仅用于认证服务维护令牌在线状态。
 */
@Data
public class ImOnlineUserVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String tokenId;

    private String userName;

    private String ipaddr;

    private String loginLocation;

    private String browser;

    private String os;

    private Long loginTime;

    private String clientKey;

    private String deviceType;

    private String deptName;

}
