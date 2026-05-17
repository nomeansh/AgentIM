package com.AgentIM.business.im.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.AgentIM.business.im.service.IImIntegrationCheckService;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * IM 后端真实联调检查接口。
 *
 * <p>该接口面向部署与测试阶段，用于确认 Business 服务与 PostgreSQL、Redis、RocketMQ 事件链路
 * 的基础连接状态。接口受 `im:ops:check` 权限保护，避免普通用户探测内部基础设施状态。</p>
 */
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/im/integration")
public class ImIntegrationCheckController {

    private final IImIntegrationCheckService integrationCheckService;

    /**
     * 查询 P0 后端关键依赖联调状态。
     *
     * <p>返回结果包含 database、redis、rocketmq 三部分。该接口不发送业务消息、不创建业务数据，
     * 仅做低副作用连通性检查，适合部署后第一时间确认环境是否满足 P0 后端运行条件。</p>
     *
     * @return 统一响应包装的联调检查结果
     */
    @SaCheckPermission("im:ops:check")
    @GetMapping("/check")
    public R<Map<String, Object>> check() {
        return R.ok(integrationCheckService.check());
    }
}
