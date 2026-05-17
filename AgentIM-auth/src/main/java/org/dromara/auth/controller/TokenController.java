package org.dromara.auth.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.auth.domain.vo.LoginVo;
import org.dromara.auth.form.RegisterBody;
import org.dromara.auth.service.ImAuthAccountService;
import org.dromara.auth.service.IAuthStrategy;
import org.dromara.auth.service.SysLoginService;
import org.dromara.common.core.domain.R;
import org.dromara.common.core.domain.model.LoginBody;
import org.dromara.common.core.utils.*;
import org.dromara.common.encrypt.annotation.ApiEncrypt;
import org.dromara.common.json.utils.JsonUtils;
import com.AgentIM.auth.api.domain.vo.RemoteClientVo;
import org.springframework.web.bind.annotation.*;

/**
 * token 控制
 *
 * @author Lion Li
 */
@Slf4j
@RequiredArgsConstructor
@RestController
public class TokenController {

    private final SysLoginService sysLoginService;
    private final ImAuthAccountService authAccountService;

    /**
     * 登录方法
     *
     * @param body 登录信息
     * @return 结果
     */
    @ApiEncrypt
    @PostMapping("/login")
    public R<LoginVo> login(@RequestBody String body) {
        LoginBody loginBody = JsonUtils.parseObject(body, LoginBody.class);
        ValidatorUtils.validate(loginBody);
        // 授权类型和客户端id
        String clientId = loginBody.getClientId();
        String grantType = loginBody.getGrantType();
        RemoteClientVo clientVo = authAccountService.resolveClient(clientId, grantType);
        LoginVo loginVo = IAuthStrategy.login(body, clientVo, grantType);
        return R.ok(loginVo);
    }

    /**
     * 登出方法
     */
    @PostMapping("logout")
    public R<Void> logout() {
        sysLoginService.logout();
        return R.ok();
    }

    /**
     * 用户注册
     */
    @ApiEncrypt
    @PostMapping("register")
    public R<Void> register(@RequestBody RegisterBody registerBody) {
        if (!authAccountService.registerEnabled()) {
            return R.fail("当前系统没有开启注册功能！");
        }
        sysLoginService.register(registerBody);
        return R.ok();
    }

}
