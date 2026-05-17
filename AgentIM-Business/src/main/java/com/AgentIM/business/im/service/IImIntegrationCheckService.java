package com.AgentIM.business.im.service;

import java.util.Map;

/**
 * IM 真实联调检查服务。
 *
 * <p>该服务用于部署后验证 P0 后端关键依赖是否真正可用，覆盖数据库、Redis 和 RocketMQ
 * 事件通道的基础连通性。它不是业务健康判断的替代品，而是给开发、测试和私有部署提供一个
 * 可直接调用的后端联调入口。</p>
 */
public interface IImIntegrationCheckService {

    /**
     * 执行核心依赖联通检查。
     *
     * <p>返回结果会按依赖拆分为多个节点，每个节点包含状态、说明和关键配置。调用方可以据此
     * 快速定位是数据库初始化、Redis 连接、RocketMQ 配置还是事件模板初始化问题。</p>
     *
     * @return 联调检查结果 Map
     */
    Map<String, Object> check();
}
