package com.AgentIM.business.auth.dubbo;

import com.AgentIM.auth.api.RemoteAuthAccountService;
import com.AgentIM.auth.api.domain.vo.RemoteClientVo;
import com.AgentIM.auth.api.model.LoginUser;
import com.AgentIM.business.im.constant.ImConstants;
import com.AgentIM.business.im.domain.bo.ImRegisterBo;
import com.AgentIM.business.im.service.IImUserService;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboService;
import org.dromara.common.core.utils.StringUtils;

import java.util.List;

/**
 * Business 提供给 Auth 服务的账号远程服务实现。
 *
 * <p>Auth 服务只负责登录入口和 Sa-Token 会话创建，账号数据、密码校验和注册落在 Business 模块，
 * 便于 IM 用户资料与登录账号保持一致。</p>
 */
@DubboService
@RequiredArgsConstructor
public class RemoteAuthAccountServiceImpl implements RemoteAuthAccountService {

    private final IImUserService userService;

    /**
     * 根据客户端和授权类型解析登录客户端。
     *
     * <p>P0 提供一个默认 web 客户端配置，允许 password 授权。后续若需要多端配置，可改为从
     * 数据库或 Nacos 配置读取。</p>
     *
     * @param clientId 客户端 ID
     * @param grantType 授权类型
     * @return 远程客户端视图
     */
    @Override
    public RemoteClientVo resolveClient(String clientId, String grantType) {
        RemoteClientVo client = new RemoteClientVo();
        client.setId(1L);
        client.setClientId(StringUtils.blankToDefault(clientId, ImConstants.DEFAULT_CLIENT_ID));
        client.setClientKey(client.getClientId());
        client.setClientSecret(ImConstants.DEFAULT_CLIENT_SECRET);
        client.setGrantType(StringUtils.blankToDefault(grantType, "password"));
        client.setGrantTypeList(List.of("password"));
        client.setDeviceType("web");
        client.setActiveTimeout(-1L);
        client.setTimeout(60L * 60L * 24L * 7L);
        client.setStatus("0");
        return client;
    }

    /**
     * 根据用户名加载登录用户。
     *
     * <p>返回对象包含密码摘要和 IM 权限集合，供 Auth 服务完成密码校验和会话创建。</p>
     *
     * @param username 用户名
     * @return 登录用户模型
     */
    @Override
    public LoginUser loadUserByUsername(String username) {
        return userService.loadLoginUser(username);
    }

    /**
     * 校验明文密码与密文密码是否匹配。
     *
     * <p>Auth 服务调用该方法时不需要理解 Business 侧密码摘要格式。</p>
     *
     * @param rawPassword 明文密码
     * @param encodedPassword 存储摘要
     * @return true 表示匹配
     */
    @Override
    public boolean passwordMatches(String rawPassword, String encodedPassword) {
        return userService.passwordMatches(rawPassword, encodedPassword);
    }

    /**
     * 是否允许开放注册。
     *
     * <p>P0 默认开放注册，方便私有化部署后直接完成用户创建和 IM 流程验证。</p>
     *
     * @return true 表示允许注册
     */
    @Override
    public boolean registerEnabled() {
        return true;
    }

    /**
     * 注册新用户账号。
     *
     * <p>注册会创建 IM 用户资料和保存的消息聊天。用户类型为空时使用 app_user。</p>
     *
     * @param username 用户名
     * @param password 明文密码
     * @param userType 用户类型
     * @return 新用户 ID
     */
    @Override
    public Long register(String username, String password, String userType) {
        ImRegisterBo bo = new ImRegisterBo();
        bo.setUsername(username);
        bo.setPassword(password);
        bo.setUserType(StringUtils.blankToDefault(userType, ImConstants.DEFAULT_USER_TYPE));
        return userService.register(bo);
    }
}
