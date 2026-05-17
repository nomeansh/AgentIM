package com.AgentIM.business.im.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.dromara.common.core.exception.ServiceException;

import java.util.Arrays;
import java.util.Set;

/**
 * 聊天成员角色。
 *
 * <p>角色决定群组管理、频道发言、成员移除、置顶和删除他人消息等数据级权限。该枚举不替代
 * Sa-Token 权限，而是在具体聊天内做二次授权。</p>
 */
@Getter
@AllArgsConstructor
public enum ImMemberRole implements ImCodeEnum {

    OWNER("owner", "所有者"),
    ADMIN("admin", "管理员"),
    MEMBER("member", "成员"),
    SUBSCRIBER("subscriber", "订阅者");

    private static final Set<String> MANAGER_ROLES = Set.of(OWNER.code, ADMIN.code);

    private final String code;
    private final String desc;

    /**
     * 根据编码解析成员角色。
     *
     * <p>成员新增或角色变更时使用该方法保证角色值来自受控集合，避免频道订阅者、群成员等权限
     * 判断出现未知分支。</p>
     *
     * @param code 待解析的角色编码
     * @return 匹配的成员角色
     */
    public static ImMemberRole require(String code) {
        return Arrays.stream(values())
            .filter(item -> item.code.equals(code))
            .findFirst()
            .orElseThrow(() -> new ServiceException("不支持的成员角色: {}", code));
    }

    /**
     * 判断角色是否具备聊天管理能力。
     *
     * <p>owner 和 admin 可以管理成员、修改聊天设置、置顶消息以及在部分场景下删除他人消息。
     * 普通成员和频道订阅者不具备这些能力。</p>
     *
     * @param role 待判断的角色编码
     * @return true 表示该角色是 owner 或 admin
     */
    public static boolean isManager(String role) {
        return MANAGER_ROLES.contains(role);
    }
}
