package org.dromara.auth.service;

import com.AgentIM.auth.api.RemoteAuthAccountService;
import com.AgentIM.auth.api.domain.vo.RemoteClientVo;
import com.AgentIM.auth.api.model.LoginUser;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Service;

/**
 * 认证中心账号服务代理。
 *
 * <p>账号、角色、客户端等身份数据由 Business 模块提供，Auth 仅负责登录入口、
 * 验证码、失败次数控制和 Sa-Token 会话创建。</p>
 */
@Service
@RequiredArgsConstructor
public class ImAuthAccountService {

    @DubboReference
    private RemoteAuthAccountService remoteAuthAccountService;

    /**
     * 根据客户端 ID 和授权类型解析登录客户端。
     *
     * <p>该方法代理 Business 远程契约，返回客户端密钥、设备类型和 token 超时配置，供具体登录
     * 策略创建 Sa-Token 会话时使用。</p>
     *
     * @param clientId 客户端 ID
     * @param grantType 授权类型
     * @return 客户端配置视图
     */
    public RemoteClientVo resolveClient(String clientId, String grantType) {
        return remoteAuthAccountService.resolveClient(clientId, grantType);
    }

    /**
     * 根据用户名加载登录用户。
     *
     * <p>Auth 服务不直接读取业务用户表，而是通过该代理获取包含密码摘要和权限集合的 LoginUser。</p>
     *
     * @param username 用户名
     * @return 登录用户模型
     */
    public LoginUser loadUserByUsername(String username) {
        return remoteAuthAccountService.loadUserByUsername(username);
    }

    /**
     * 校验登录密码。
     *
     * <p>密码摘要算法由 Business 模块掌握，Auth 只传入明文密码和 LoginUser 中的存储摘要。</p>
     *
     * @param rawPassword 明文密码
     * @param user 登录用户模型
     * @return true 表示密码匹配
     */
    public boolean passwordMatches(String rawPassword, LoginUser user) {
        return remoteAuthAccountService.passwordMatches(rawPassword, user.getPassword());
    }

    /**
     * 查询是否允许开放注册。
     *
     * <p>注册入口先调用该方法，使 Business 可以通过配置集中控制是否开放新用户注册。</p>
     *
     * @return true 表示允许注册
     */
    public boolean registerEnabled() {
        return remoteAuthAccountService.registerEnabled();
    }

    /**
     * 注册新用户。
     *
     * <p>方法把注册表单中的用户名、密码和用户类型转交给 Business 模块。Business 会负责账号资料
     * 和保存的消息会话创建。</p>
     *
     * @param username 用户名
     * @param password 明文密码
     * @param userType 用户类型
     * @return 新用户 ID
     */
    public Long register(String username, String password, String userType) {
        return remoteAuthAccountService.register(username, password, userType);
    }

}
