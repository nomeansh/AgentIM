# 08 — Webhook 与斜杠命令

> **Webhook 属于 P2 DevOps 集成范围。斜杠命令可以分阶段引入，P0 只保留不依赖 Workspace 的基础 IM 命令。**

## 概述

Webhook 和斜杠命令是 AgentIM 的"外部输入通道"：

- **Webhook** — 外部系统（GitHub、GitLab、CI）通过 HTTP 回调通知 AgentIM 事件发生
- **斜杠命令** — 用户在消息输入框以 `/` 开头的快捷操作

## 斜杠命令

斜杠命令是用户在聊天输入框中输入的特殊命令，以 `/` 开头，触发预定义的操作。

### P0 基础 IM 命令

P0 阶段只实现与当前聊天直接相关的基础命令，不引入 Workspace 概念：

```
/channel create <名称>           ← 创建频道
/channel invite @user            ← 邀请用户加入频道
/message pin <messageId>         ← 置顶消息
/help                            ← 查看可用命令
```

### P1 Agent 命令

P1 阶段扩展 Agent 相关命令：

```
/task create <标题>              ← 快速创建任务
/task assign <taskId> @user      ← 分配任务
/task list                       ← 查看任务列表
/agent run <指令>                 ← 触发 Agent 执行
/agent pause [runId]             ← 暂停 Agent
/agent resume [runId]            ← 恢复 Agent
/agent status [runId]            ← 查看执行状态
```

### P2 DevOps 命令

P2 阶段扩展 DevOps 相关命令：

```
/repo list                       ← 查看关联仓库
/repo connect <url>              ← 连接代码仓库
/pr list [repo]                  ← 查看 PR 列表
/ci status [repo]                ← 查看 CI 状态
```

### CommandHandler 接口设计

```java
public interface SlashCommandHandler {
    /** 命令名称，如 "channel" 或 "repo"（不含 /） */
    String command();
    
    /** 处理命令 */
    CommandResult handle(CommandContext context);
}

public class CommandContext {
    private String command;           // 命令名
    private List<String> args;        // 参数列表
    private Long workspaceId;         // 当前工作区（P1/P2 可选）
    private Long channelId;           // 当前频道
    private Long userId;              // 执行用户
}

public class CommandResult {
    private boolean success;
    private String message;           // 返回给用户的消息
    private Object data;              // 可选数据
}
```

**命令处理要求：**
- 命令必须幂等（重复执行不产生副作用）
- 失败时返回人类可理解的错误信息
- 未知命令返回 `/help` 提示

## Webhook

Webhook 是 AgentIM 与外部系统（GitHub、GitLab、CI 工具）的集成通道。

### P2 Webhook 接入范围

| Webhook 类型 | 事件 | 说明 |
|-------------|------|------|
| GitHub / GitLab | `push` | 代码推送 |
| | `pull_request.opened` | PR 创建 |
| | `pull_request.updated` | PR 更新 |
| | `pull_request.merged` | PR 合并 |
| | `issues.opened` | Issue 创建 |
| | `issues.updated` | Issue 更新 |
| CI（Jenkins / GitHub Actions / GitLab CI） | `ci.success` | CI 成功 |
| | `ci.failed` | CI 失败 |

### Webhook 处理流程

```
外部系统（GitHub）推送事件
  │
  POST /devops/webhooks/github
  │
  ├── 1. 校验签名（HMAC-SHA256）
  │     └→ 签名不匹配 → 返回 401，不处理
  │
  ├── 2. 写入原始事件到 devops_webhook_event
  │     └→ provider + external_event_id 唯一约束
  │
  ├── 3. 幂等去重
  │     └→ 如果相同 provider + external_event_id 已存在 → 直接返回
  │
  ├── 4. 转换为内部领域事件
  │     └→ GitHub PR opened → 内部 pull_request.opened 事件
  │
  ├── 5. 更新本地镜像表
  │     └→ 创建/更新 devops_repo / devops_pull_request / devops_issue / devops_ci_run
  │
  ├── 6. 查找关联的 Workspace 和频道
  │     └→ 通过 devops_repo 的 workspace_id 关联
  │
  └── 7. 发送系统通知到频道
        └→ 生成 PR 卡片 / CI 结果卡片
        └→ 通过 WebSocket 推送
```

### 幂等去重设计

```java
public void handleWebhook(WebhookRequest request) {
    // 1. 校验签名
    verifySignature(request);
    
    // 2. 幂等 Key：provider + external_event_id
    String eventKey = request.getProvider() + ":" + request.getExternalEventId();
    
    // 3. 检查是否已处理
    if (webhookEventMapper.existsByEventKey(eventKey)) {
        return; // 已处理，跳过
    }
    
    // 4. 保存原始事件
    saveRawEvent(request);
    
    // 5. 转换并发布领域事件
    DomainEvent event = converter.convert(request);
    publishAfterCommit(event);
}
```

### 内部事件清单

P2 Webhook 处理后发布的内部事件：

| 内部事件 | 触发条件 | 后续处理 |
|----------|----------|----------|
| `repo.push.received` | 代码推送 | 更新 repo 表，通知频道 |
| `pull_request.opened` | PR 创建 | 创建 PR 镜像，发送卡片通知 |
| `pull_request.updated` | PR 更新 | 更新 PR 镜像 |
| `pull_request.merged` | PR 合并 | 更新 PR 状态，通知频道 |
| `issue.created` | Issue 创建 | 创建 Issue 镜像，通知频道 |
| `issue.updated` | Issue 更新 | 更新 Issue 镜像 |
| `ci.run.failed` | CI 失败 | 更新 CI 状态，可触发 Agent 分析 |
| `ci.run.succeeded` | CI 成功 | 更新 CI 状态，通知频道 |

### Webhook 安全

```
1. 签名校验：
   - GitHub: X-Hub-Signature-256 (HMAC-SHA256)
   - GitLab: X-Gitlab-Token
   - 所有签名不匹配的请求直接拒绝

2. 限流：
   - 单 IP 每秒最多 10 次请求
   - 超过限流返回 429

3. 存储加密：
   - Webhook Secret 加密存储
   - 外部 Token 加密存储

4. 审计：
   - 所有 Webhook 请求记录审计日志
   - 处理失败记录原始 payload 用于排障
```

### 与 P1 Agent 的联动

P2 的 Webhook 事件可以和 P1 的 Agent 运行时联动：

```
CI 失败事件
  → Webhook 处理
  → 找到关联 Workspace
  → 可选：自动触发 Agent Run
  → Agent 分析 CI 日志
  → 生成修复建议
  → 创建 Issue 或 PR 评论
  → 通知频道
```

这种联动不是 P0/P1 的必做范围，但展示了 P1 + P2 组合后的完整能力。
