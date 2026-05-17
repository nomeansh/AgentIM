package com.AgentIM.auth.api;

import com.AgentIM.auth.api.domain.vo.RemoteClientVo;
import com.AgentIM.auth.api.model.LoginUser;

/**
 * Business 提供给认证中心的账号与客户端契约。
 *
 * <p>认证中心只负责登录入口、验证码和 Sa-Token 会话创建。账号资料、密码摘要、注册和客户端
 * 配置由 Business 模块统一提供，确保 IM 用户资料与登录身份保持同源。</p>
 */
public interface RemoteAuthAccountService {

    /**
     * 根据客户端和授权类型解析登录客户端。
     *
     * <p>Auth 服务在创建登录策略前调用该方法获取客户端密钥、授权类型、设备类型和 token
     * 过期策略。P0 可返回默认客户端，后续可扩展为数据库配置。</p>
     *
     * @param clientId 客户端 ID
     * @param grantType 授权类型
     * @return 客户端远程视图
     */
    RemoteClientVo resolveClient(String clientId, String grantType);

    /**
     * 根据用户名加载登录用户。
     *
     * <p>返回对象包含用户 ID、用户名、昵称、密码摘要和权限集合。Auth 服务不直接访问 IM 用户表，
     * 避免认证模块与业务表结构耦合。</p>
     *
     * @param username 用户名
     * @return 登录用户模型
     */
    LoginUser loadUserByUsername(String username);

    /**
     * 校验明文密码与密文密码是否匹配。
     *
     * <p>密码摘要算法归 Business 模块管理。Auth 服务只传入明文密码和存储摘要，由 Business 判断
     * 是否匹配。</p>
     *
     * @param rawPassword 明文密码
     * @param encodedPassword 存储密码摘要
     * @return true 表示匹配
     */
    boolean passwordMatches(String rawPassword, String encodedPassword);

    /**
     * 是否允许开放注册。
     *
     * <p>Auth 注册入口在写入账号前调用该方法。Business 可基于配置、部署模式或安全策略决定是否
     * 开启公开注册。</p>
     *
     * @return true 表示允许注册
     */
    boolean registerEnabled();

    /**
     * 注册新用户账号。
     *
     * <p>该方法由 Auth 服务注册入口调用。Business 负责创建 IM 用户资料、密码摘要和保存的消息
     * 聊天，保证注册后用户立即具备 P0 IM 基础能力。</p>
     *
     * @param username 用户名
     * @param password 明文密码
     * @param userType 用户类型，可为空，Business 会使用默认类型
     * @return 新用户 ID
     */
    Long register(String username, String password, String userType);

}
