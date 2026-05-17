package com.AgentIM.auth.api.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 登录客户端视图对象。
 */
@Data
public class RemoteClientVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    private Long id;

    /**
     * 客户端 id
     */
    private String clientId;

    /**
     * 客户端 key
     */
    private String clientKey;

    /**
     * 客户端秘钥
     */
    private String clientSecret;

    /**
     * 授权类型列表
     */
    private List<String> grantTypeList;

    /**
     * 授权类型
     */
    private String grantType;

    /**
     * 设备类型
     */
    private String deviceType;

    /**
     * token 活跃超时时间
     */
    private Long activeTimeout;

    /**
     * token 固定超时时间
     */
    private Long timeout;

    /**
     * 状态（0 正常，1 停用）
     */
    private String status;

}
