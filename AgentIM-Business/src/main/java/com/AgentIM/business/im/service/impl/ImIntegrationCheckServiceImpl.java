package com.AgentIM.business.im.service.impl;

import com.AgentIM.business.im.config.ImEventProperties;
import com.AgentIM.business.im.mapper.ImUserMapper;
import com.AgentIM.business.im.service.IImIntegrationCheckService;
import lombok.RequiredArgsConstructor;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.dromara.common.redis.utils.RedisUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * IM 真实联调检查服务实现。
 *
 * <p>该实现只执行低副作用检查：数据库只做轻量读取，Redis 使用短 TTL 临时键并立即删除，
 * RocketMQ 默认只检查配置和模板是否可用，不主动发送探测消息，避免在联调接口中制造业务事件。</p>
 */
@Service
@RequiredArgsConstructor
public class ImIntegrationCheckServiceImpl implements IImIntegrationCheckService {

    private final ImUserMapper userMapper;

    private final ImEventProperties eventProperties;

    private final ObjectProvider<RocketMQTemplate> rocketMQTemplateProvider;

    /**
     * 执行核心依赖联通检查。
     *
     * <p>检查结果按 database、redis、rocketmq 三个键返回。每个节点独立捕获异常，单个依赖失败
     * 不会阻断其他依赖检查，便于一次请求看到完整联调状态。</p>
     *
     * @return 联调检查结果 Map
     */
    @Override
    public Map<String, Object> check() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("database", checkDatabase());
        result.put("redis", checkRedis());
        result.put("rocketmq", checkRocketMq());
        return result;
    }

    /**
     * 检查 PostgreSQL 与 P0 核心表是否可访问。
     *
     * <p>方法通过 `im_user` 表执行轻量读取。若数据库连接异常、schema 未初始化或权限不足，异常会
     * 被捕获并写入返回结果，方便联调阶段直接定位数据库侧问题。</p>
     *
     * @return 数据库检查结果
     */
    private Map<String, Object> checkDatabase() {
        try {
            userMapper.checkImUserTable();
            return status(true, "PostgreSQL 连接正常，im_user 表可访问");
        } catch (Exception e) {
            return status(false, "PostgreSQL 或 im_user 表检查失败: " + e.getMessage());
        }
    }

    /**
     * 检查 Redis 是否支持基础读写。
     *
     * <p>方法写入一个 30 秒 TTL 的临时键，读取后立即删除。该检查能覆盖 Redis 连接、序列化和
     * 基础命令权限，失败时通常表示 Redis 地址、密码、网络或 Redisson 配置存在问题。</p>
     *
     * @return Redis 检查结果
     */
    private Map<String, Object> checkRedis() {
        String key = "agentim:im:integration:" + UUID.randomUUID();
        String value = "ok";
        try {
            RedisUtils.setCacheObject(key, value, Duration.ofSeconds(30));
            String cached = RedisUtils.getCacheObject(key);
            RedisUtils.deleteObject(key);
            boolean ok = value.equals(cached);
            return status(ok, ok ? "Redis 读写正常" : "Redis 读写返回值不一致");
        } catch (Exception e) {
            return status(false, "Redis 检查失败: " + e.getMessage());
        }
    }

    /**
     * 检查 RocketMQ 事件链路配置。
     *
     * <p>当 RocketMQ 未启用时返回跳过状态，表示当前部署仍使用 WebSocket 直发。启用后会检查
     * RocketMQTemplate 是否存在，并返回当前 destination，便于确认 name-server、producer 和
     * topic/tag 配置是否已经进入 Spring 容器。</p>
     *
     * @return RocketMQ 检查结果
     */
    private Map<String, Object> checkRocketMq() {
        Map<String, Object> result = new LinkedHashMap<>();
        boolean enabled = eventProperties.getRocketmq().isEnabled();
        result.put("enabled", enabled);
        result.put("destination", eventProperties.rocketMqDestination());
        if (!enabled) {
            result.put("ok", true);
            result.put("message", "RocketMQ 未启用，当前使用事务后 WebSocket 直发");
            return result;
        }
        RocketMQTemplate template = rocketMQTemplateProvider.getIfAvailable();
        result.put("ok", template != null);
        result.put("message", template != null ? "RocketMQTemplate 已初始化" : "RocketMQ 已启用但 RocketMQTemplate 未初始化");
        return result;
    }

    /**
     * 构建统一检查节点结果。
     *
     * <p>联调接口返回 Map 而不是新增复杂 VO，是为了便于后续追加依赖节点时保持接口兼容。</p>
     *
     * @param ok 检查是否通过
     * @param message 检查说明
     * @return 检查节点结果
     */
    private Map<String, Object> status(boolean ok, String message) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("ok", ok);
        result.put("message", message);
        return result;
    }
}
