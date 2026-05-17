# AgentIM

**开源即时通讯平台** — 像 Telegram 一样流畅的云消息体验，可私有部署。

## 项目定位

AgentIM 是一个面向个人与团队的开源即时通讯平台，提供：

- **云消息** — 所有消息存储在云端，多设备同步
- **私聊 / 群组 / 频道** — 覆盖全部沟通场景
- **富媒体消息** — 文本、图片、文件、语音、视频、投票
- **频道广播** — 一对多订阅，适合公告和通知
- **多设备** — 手机、桌面、Web 同时在线，状态同步

## 阶段规划

| 阶段 | 聚焦 | 状态 |
|------|------|------|
| **P0 — 核心 IM** | Telegram 式云消息平台 | 🚧 开发中 |
| **P1 — 增强协作与社区** | 工作区、超级群、话题、状态、语音房、Agent 协作 | 📋 规划中 |
| **P2 — 开放生态与增长** | Bot API、Mini Apps、工作流、公开频道、DevOps、企业治理 | 📋 规划中 |

> 文档已按阶段拆分：P0 核心 IM 位于 `AgentIM_Docs/p0/`，P1 增强协作与 Agent 运行时位于 `AgentIM_Docs/p1/`，P2 开放生态与 DevOps 集成位于 `AgentIM_Docs/p2/`。

## 技术栈

| 组件 | 技术 |
|------|------|
| 基础框架 | RuoYi-Cloud-Plus (Spring Cloud) |
| 服务网关 | Spring Cloud Gateway |
| 认证授权 | Sa-Token |
| 数据库 | PostgreSQL 16 |
| 缓存 | Redis |
| 消息队列 | RocketMQ |
| 搜索引擎 | Elasticsearch |
| 对象存储 | MinIO / OSS / S3 |
| 实时通信 | WebSocket |
| 配置中心 | Nacos |

## 快速开始

### 环境要求

- JDK 21+
- PostgreSQL 16
- Redis 7.0+
- RocketMQ 5.3+
- Elasticsearch 8.16+
- Nacos 2.3.x
- MinIO（可选）

### 启动步骤

1. 启动 Nacos、PostgreSQL、Redis、RocketMQ、Elasticsearch
2. 创建数据库 `agentim` 并执行建表脚本
3. 启动 `AgentIM-gateway`（网关）
4. 启动 `AgentIM-auth`（认证服务）
5. 启动 `AgentIM-Business`（业务服务）

## 项目结构

```
AgentIM-Business/          ← 主业务服务
AgentIM-gateway/           ← 网关
AgentIM-auth/              ← 认证服务
AgentIM-modules/           ← 模块（资源服务）
AgentIM-api/               ← Dubbo 远程接口
AgentIM-common/            ← 公共能力（WebSocket/SSE/Redis/ES）
AgentIM_Docs/              ← 设计文档
  p0/                      ← 核心 IM 设计与实施文档
  p1/                      ← 增强协作、社区、Agent 运行时规划
  p2/                      ← 开放平台、工作流、Webhook / DevOps 集成规划
```

## 文档

详细设计文档在 `AgentIM_Docs/` 目录下，建议从 `00_阅读说明.md` 开始阅读。

## License

参考父项目 RuoYi-Cloud-Plus 的 License。
