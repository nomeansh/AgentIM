package com.AgentIM.business.im.constant;

import java.util.Set;

/**
 * IM 业务常量集合。
 *
 * <p>该类集中保存 P0 后端在权限、默认客户端、删除标记和登录上下文中反复使用的稳定值。
 * 常量集中管理可以避免 Controller、Service 和 Dubbo 契约之间出现字符串漂移。</p>
 */
public final class ImConstants {

    /**
     * 正常数据逻辑删除标记。
     */
    public static final String DEL_FLAG_NORMAL = "0";

    /**
     * 已删除数据逻辑删除标记。
     */
    public static final String DEL_FLAG_DELETED = "2";

    /**
     * 默认登录租户上下文。AgentIM 当前 P0 不引入多租户产品能力，但 Sa-Token 登录模型仍需要租户字段。
     */
    public static final String DEFAULT_TENANT_ID = "000000";

    /**
     * 默认登录客户端 ID。当前认证中心通过 Business 解析客户端，P0 提供 web 客户端的最小可用配置。
     */
    public static final String DEFAULT_CLIENT_ID = "agentim-web";

    /**
     * 默认客户端密钥。P0 登录策略未强制校验客户端密钥，该值用于填充远程客户端契约。
     */
    public static final String DEFAULT_CLIENT_SECRET = "agentim";

    /**
     * 默认用户类型，匹配 RuoYi 公共 UserType.APP_USER。
     */
    public static final String DEFAULT_USER_TYPE = "app_user";

    /**
     * P0 所有 IM 菜单权限集合。登录用户携带这些权限后，Sa-Token 注解可直接放行 IM 接口。
     */
    public static final Set<String> P0_PERMISSIONS = Set.of(
        "im:user:profile",
        "im:contact:list",
        "im:contact:add",
        "im:contact:remove",
        "im:chat:list",
        "im:chat:create",
        "im:chat:update",
        "im:chat:delete",
        "im:chat:member",
        "im:message:send",
        "im:message:read",
        "im:message:edit",
        "im:message:delete",
        "im:message:pin",
        "im:reaction:add",
        "im:resource:upload"
    );

    /**
     * P0 默认角色权限。
     */
    public static final Set<String> P0_ROLES = Set.of("im_user");

    /**
     * 阻止实例化常量类。
     *
     * <p>常量类只提供静态字段，不应该被 Spring 或业务代码创建实例。构造器私有化能在编译期
     * 表达这一约束。</p>
     */
    private ImConstants() {
    }
}
