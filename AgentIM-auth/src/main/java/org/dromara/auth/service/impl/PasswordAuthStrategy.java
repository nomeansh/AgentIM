package org.dromara.auth.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.stp.parameter.SaLoginParameter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.auth.domain.vo.LoginVo;
import org.dromara.auth.form.PasswordLoginBody;
import org.dromara.auth.properties.CaptchaProperties;
import org.dromara.auth.service.ImAuthAccountService;
import org.dromara.auth.service.IAuthStrategy;
import org.dromara.auth.service.SysLoginService;
import org.dromara.common.core.constant.Constants;
import org.dromara.common.core.constant.GlobalConstants;
import org.dromara.common.core.enums.LoginType;
import org.dromara.common.core.exception.user.CaptchaException;
import org.dromara.common.core.exception.user.CaptchaExpireException;
import org.dromara.common.core.utils.MessageUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.core.utils.ValidatorUtils;
import org.dromara.common.json.utils.JsonUtils;
import org.dromara.common.redis.utils.RedisUtils;
import org.dromara.common.satoken.utils.LoginHelper;
import com.AgentIM.auth.api.domain.vo.RemoteClientVo;
import com.AgentIM.auth.api.model.LoginUser;
import org.springframework.stereotype.Service;

/**
 * 密码认证策略
 *
 * @author Michelle.Chung
 */
@Slf4j
@Service("password" + IAuthStrategy.BASE_NAME)
@RequiredArgsConstructor
public class PasswordAuthStrategy implements IAuthStrategy {

    private static final String DEFAULT_LOGIN_CONTEXT_ID = "000000";

    private final CaptchaProperties captchaProperties;

    private final SysLoginService loginService;

    private final ImAuthAccountService authAccountService;

    /**
     * 使用账号密码完成 AgentIM 用户登录。
     *
     * <p>该方法是 OAuth 密码登录策略在 Auth 服务中的入口。它负责把网关传入的 JSON
     * 登录体解析为 {@link PasswordLoginBody}，按租户上下文校验验证码，再通过
     * {@link ImAuthAccountService} 调用 Business 服务加载 IM 用户与校验密码。认证通过后，
     * 方法会把客户端标识、设备类型、访问令牌过期时间等客户端配置写入
     * {@link SaLoginParameter}，最终交给 Sa-Token 生成访问令牌。</p>
     *
     * <p>P0 阶段登录主体来自核心 IM 用户表，因此这里不再查询传统后台用户体系；登录成功后
     * 返回的 {@link LoginVo} 只暴露客户端需要的令牌、过期时间和客户端编号，用户资料由
     * Business 的 IM 用户接口按登录态获取。</p>
     *
     * @param body 网关传入的密码登录 JSON 字符串，必须能反序列化为 {@link PasswordLoginBody}
     * @param client 已解析的 OAuth 客户端配置，提供 token 时效、设备类型和客户端标识
     * @return 登录成功后的访问令牌视图，包含 accessToken、expireIn 和 clientId
     */
    @Override
    public LoginVo login(String body, RemoteClientVo client) {
        PasswordLoginBody loginBody = JsonUtils.parseObject(body, PasswordLoginBody.class);
        ValidatorUtils.validate(loginBody);
        String tenantId = DEFAULT_LOGIN_CONTEXT_ID;
        String username = loginBody.getUsername();
        String password = loginBody.getPassword();
        String code = loginBody.getCode();
        String uuid = loginBody.getUuid();

        // 验证码开关
        if (captchaProperties.getEnabled()) {
            validateCaptcha(tenantId, username, code, uuid);
        }
        LoginUser loginUser = authAccountService.loadUserByUsername(username);
        loginService.checkLogin(LoginType.PASSWORD, tenantId, username, () -> !authAccountService.passwordMatches(password, loginUser));
        loginUser.setClientKey(client.getClientKey());
        loginUser.setDeviceType(client.getDeviceType());
        SaLoginParameter model = new SaLoginParameter();
        model.setDeviceType(client.getDeviceType());
        // 自定义分配 不同用户体系 不同 token 授权时间 不设置默认走全局 yml 配置
        // 例如: 后台用户30分钟过期 app用户1天过期
        model.setTimeout(client.getTimeout());
        model.setActiveTimeout(client.getActiveTimeout());
        model.setExtra(LoginHelper.CLIENT_KEY, client.getClientId());
        // 生成token
        LoginHelper.login(loginUser, model);

        LoginVo loginVo = new LoginVo();
        loginVo.setAccessToken(StpUtil.getTokenValue());
        loginVo.setExpireIn(StpUtil.getTokenTimeout());
        loginVo.setClientId(client.getClientId());
        return loginVo;
    }

    /**
     * 校验密码登录验证码并记录失败审计信息。
     *
     * <p>验证码以 UUID 组合 Redis key 存储，读取后立即删除以保证一次性使用语义。验证码缺失
     * 表示已过期或客户端传入了错误 UUID；验证码不匹配表示用户输入错误。两类失败都会先调用
     * {@link SysLoginService#recordLogininfor(String, String, String, String)} 写入登录日志，
     * 再抛出 RuoYi 既有认证异常，保持 Auth 模块原有错误处理链路。</p>
     *
     * @param tenantId 当前登录上下文租户标识，P0 IM 默认使用固定上下文以兼容现有日志结构
     * @param username 用户名
     * @param code     验证码
     * @param uuid     唯一标识
     */
    private void validateCaptcha(String tenantId, String username, String code, String uuid) {
        String verifyKey = GlobalConstants.CAPTCHA_CODE_KEY + StringUtils.blankToDefault(uuid, "");
        String captcha = RedisUtils.getCacheObject(verifyKey);
        RedisUtils.deleteObject(verifyKey);
        if (captcha == null) {
            loginService.recordLogininfor(tenantId, username, Constants.LOGIN_FAIL, MessageUtils.message("user.jcaptcha.expire"));
            throw new CaptchaExpireException();
        }
        if (!StringUtils.equalsIgnoreCase(code, captcha)) {
            loginService.recordLogininfor(tenantId, username, Constants.LOGIN_FAIL, MessageUtils.message("user.jcaptcha.error"));
            throw new CaptchaException();
        }
    }

}
