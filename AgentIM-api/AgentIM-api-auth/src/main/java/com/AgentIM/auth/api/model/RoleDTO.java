package com.AgentIM.auth.api.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 角色信息。
 */
@Data
@NoArgsConstructor
public class RoleDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 角色 ID
     */
    private Long roleId;

    /**
     * 角色名称
     */
    private String roleName;

    /**
     * 角色权限
     */
    private String roleKey;

    /**
     * 数据范围。
     */
    private String dataScope;

}
