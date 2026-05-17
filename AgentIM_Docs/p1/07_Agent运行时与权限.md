# 07 — Agent 运行时与权限

> **本文档属于 P1 AgentIM 核心范围。P0 基础 IM 阶段不实现本章内容。**

## 概述

Agent 运行时是 AgentIM 区别于普通 IM 的核心能力。它不是简单的"AI 聊天机器人"，而是一个**可追踪、可审批、可沉淀**的 Agent 执行引擎。

### 与普通聊天机器人的区别

| 维度 | 普通聊天机器人 | AgentIM Agent |
|------|---------------|---------------|
| 执行记录 | 对话历史（无结构） | `agent_run` + `agent_run_step` + `agent_tool_call`（结构化可追溯） |
| 工具调用 | 黑盒，用户看不到中间步骤 | 每步工具调用记录输入/输出/耗时/状态 |
| 高风险操作 | 无审批，直接执行 | 创建 `agent_approval`，等待人工审批 |
| 产出沉淀 | 留在聊天文本中 | 生成 `Artifact`，有版本管理 |
| 执行控制 | 无法暂停/取消 | 支持 pause / resume / cancel |
| 权限体系 | 无或简单 | 6 层检查 + Workspace 级别策略 |

## Agent Run 生命周期

Agent Run（执行记录）是一个完整的状态机：

```
创建时：
  QUEUED（排队中）
    │
    ▼
  RUNNING（执行中）←──────────────┐
    │                              │
    ├──→ WAITING_APPROVAL（等待审批）┐
    │         │                    │
    │  用户审批通过                 │
    │         │                    │
    │         └──→ RUNNING ────────┘
    │                              │
    ├──→ PAUSED（已暂停）────────────┘
    │   用户恢复
    │
    ├──→ SUCCEEDED（成功）
    ├──→ FAILED（失败）
    ├──→ CANCELLED（已取消）
    └──→ REJECTED（审批拒绝）
```

### 状态含义

| 状态 | 含义 | 谁驱动 |
|------|------|--------|
| `QUEUED` | Agent Run 已创建，等待 Runtime 领取 | 系统 |
| `RUNNING` | Runtime 正在依次执行 Step | Runtime |
| `WAITING_APPROVAL` | 执行到高风险操作，等待审批 | 用户 |
| `PAUSED` | 用户手动暂停 | 用户 |
| `SUCCEEDED` | 所有 Step 执行完成 | Runtime |
| `FAILED` | 执行过程中出现不可恢复错误 | Runtime |
| `CANCELLED` | 用户取消执行 | 用户 |
| `REJECTED` | 审批被拒绝，执行终止 | 用户 |

## Agent Run 创建与执行流程

### 完整执行场景

以下是一个完整的 Agent 执行场景——用户在频道中说："帮我梳理一下用户登录模块的代码设计，找出安全隐患"：

```
步骤 1：用户触发
  └→ POST /agent/workspaces/{workspaceId}/runs
     └→ 创建 agent_run（status=QUEUED）
     └→ 创建 agent_run_step（planner 生成步骤计划）
     └→ 发布 agent.run.created 事件

步骤 2：Runtime 领取执行
  └→ 状态推进 RUNNING
  └→ 取出第一个 Step
  
步骤 3：Step 1 — 搜索代码
  └→ agent_tool_call: repo.search("login")
  └→ 记录输入："搜索 login 相关代码"
  └→ 记录输出："找到 UserController.java, AuthService.java"
  └→ 状态：succeeded

步骤 4：Step 2 — 读取代码文件
  └→ agent_tool_call: repo.read("UserController.java")
  └→ 记录输入："读取 UserController.java"
  └→ 记录输出："文件内容摘要..."
  └→ 状态：succeeded

步骤 5：Step 3 — 分析安全隐患
  └→ agent_tool_call（无外部调用，纯 LLM 分析）
  └→ 这一步是"高风险"吗？→ 只是读取，风险低 → 自动执行
  └→ 输出分析结果

步骤 6：Step 4 — 创建 Artifact
  └→ agent_tool_call: artifact.write
  └→ 生成 "登录模块安全审计报告" Artifact
  └→ 状态：succeeded

步骤 7：完成
  └→ 状态推进 SUCCEEDED
  └→ 生成 Artifact 摘要
  └→ 回写频道消息："已生成登录模块安全审计报告"
  └→ 发布 agent.run.completed 事件
```

### 如果遇到高风险操作

假设 Agent 认为需要修改代码来修复安全漏洞：

```
步骤 6：Step 4 — 创建修复 PR
  └→ agent_tool_call: pr.create
  └→ 检查工具权限策略：pr.create → approval_required
  └→ 创建 agent_approval（status=pending）
  └→ 状态推进 WAITING_APPROVAL
  └→ 发布 agent.run.waiting_approval 事件
  └→ 用户收到审批通知（卡片消息）

用户审批通过：
  └→ POST /agent/approvals/{approvalId}/approve
  └→ 更新审批状态为 APPROVED
  └→ Agent Run 恢复 RUNNING
  └→ 继续执行工具调用

用户审批拒绝：
  └→ POST /agent/approvals/{approvalId}/reject
  └→ 更新审批状态为 REJECTED
  └→ Agent Run 进入 FAILED（或要求 Agent 重新规划）
```

## 工具调用模型

### agent_tool_call 表

每次 Agent 调用外部工具都写入一条记录：

| 字段 | 说明 | 示例 |
|------|------|------|
| `run_id` | 所属执行 | 180000000000001 |
| `step_id` | 所属步骤 | 180000000000002 |
| `tool_name` | 工具名 | `repo.read` |
| `input_summary` | 输入摘要 | "读取 UserController.java" |
| `output_summary` | 输出摘要 | "返回 342 行代码" |
| `status` | 状态 | `running` / `succeeded` / `failed` |
| `error_message` | 错误信息 | "文件不存在" |
| `started_time` | 开始时间 | |
| `finished_time` | 结束时间 | |

### Agent 模式

Agent 有 5 种执行模式，决定了 Agent 的自主程度：

| 模式 | 行为 | 适用场景 |
|------|------|----------|
| `CHAT_ONLY` | 只回答问题，不能创建任务/产物 | 快速问答、知识查询 |
| `SUGGEST` | 可创建草稿任务/建议，不执行任何外部动作 | 方案讨论、头脑风暴 |
| `DRAFT` | 可生成草稿产物，不执行外部动作 | 写文档、写设计稿 |
| `ACT_WITH_APPROVAL` | 低风险自动执行，高风险需要审批 | 日常开发辅助（默认模式） |
| `AUTOPILOT` | 仅在明确白名单范围内自动执行所有操作 | CI 自动修复、定时任务 |

## 权限检查体系

### 6 层权限检查

Agent 每调用一个工具之前，必须依次通过以下检查：

```
第 1 层：Workspace 访问权限
  └→ 用户是否有权访问该 Workspace？
  └→ 检查：workspace_member 表

第 2 层：Agent 触发权限
  └→ 用户是否有 agent:run:create 权限？
  └→ 检查：Sa-Token @SaCheckPermission

第 3 层：工具许可
  └→ 该工具在当前 Workspace 是否被允许？
  └→ 检查：agent_permission_policy 表
  └→ 结果：allowed / approval_required / denied

第 4 层：审批必要性
  └→ 如果策略要求审批，创建审批记录
  └→ 等待用户决策

第 5 层：外部集成授权
  └→ 如果操作外部系统（GitHub、GitLab 等）
  └→ 检查外部 Token 是否有效且未过期

第 6 层：操作风险等级
  └→ 写操作检查风险等级
  └→ 高风险（生产发布、删除文件）→ 即使策略是 allowed 也要求审批
```

### 工具权限矩阵

| 工具 | 默认策略 | 可被 Workspace 覆盖 |
|------|----------|-------------------|
| `message.send` | ✅ allowed | 否（基操） |
| `artifact.write` | ✅ allowed | 否 |
| `repo.read` | ✅ allowed | 是 |
| `repo.write` | 🔒 approval_required | 是（可提升为 denied） |
| `pr.create` | 🔒 approval_required | 是 |
| `pr.merge` | ❌ denied | 是（建议保留 denied） |
| `ci.retry` | 🔒 approval_required | 是 |
| `deploy.production` | ❌ denied | 建议保留 denied |

### 高风险动作清单

以下动作**默认必须审批**（即使工具策略是 allowed）：

- 修改代码或文件（`repo.write`）
- 创建 PR（`pr.create`）
- 重跑或修改 CI（`ci.retry`）
- 发布或回滚环境（`deploy.*`）
- 删除文件（任何上下文）
- 修改权限配置
- 发送外部邮件
- 修改客户数据

## Approval 处理

### 审批创建

```java
// 在 beforeToolCall 中触发
public void beforeToolCall(ToolCallRequest request) {
    // ... 前 5 层检查 ...
    
    if (policy.isApprovalRequired()) {
        // 创建审批记录
        AgentApproval approval = new AgentApproval();
        approval.setRunId(request.getRunId());
        approval.setActionType(request.getToolName());
        approval.setTitle("申请修改 UserController.java");
        approval.setSummary("Agent 在分析安全漏洞时，发现了一个 SQL 注入风险，需要修改 UserController.java 第 42 行");
        approval.setRiskLevel("medium");
        approval.setStatus("pending");
        approvalService.insert(approval);
        
        // 推进 Run 状态
        agentRunService.updateStatus(request.getRunId(), "WAITING_APPROVAL");
        
        // 发送审批通知
        eventPublisher.publishEvent("agent.run.waiting_approval", ...);
        
        // 抛异常中断当前工具调用
        throw new WaitingApprovalException();
    }
}
```

### 审批决策

```java
// 用户审批通过
POST /agent/approvals/{approvalId}/approve
→ 更新 agent_approval.status = "approved"
→ 写审计日志
→ 发布 approval.approved 事件
→ Agent Run 恢复执行（RU NNING）
→ 继续执行被中断的工具调用

// 用户审批拒绝
POST /agent/approvals/{approvalId}/reject
→ 更新 agent_approval.status = "rejected"
→ 写审计日志
→ 发布 approval.rejected 事件
→ Agent Run 进入 FAILED 状态
→ 通知频道：审批已拒绝

// 用户要求修改
POST /agent/approvals/{approvalId}/request-changes
→ 更新 agent_approval.status = "change_requested"
→ Agent 收到修改意见后重新规划步骤
→ 提交新的执行计划
```

## SSE 执行日志流

Agent 执行过程中，前端通过 SSE 实时获取执行状态：

```
客户端：
  GET /agent/runs/{runId}/stream
  → text/event-stream

事件流（按顺序推送）：
  event: run.status
  data: {"status": "RUNNING", "step": 1}
  
  event: step.started
  data: {"stepId": 1, "toolName": "repo.search"}
  
  event: tool_call.started
  data: {"toolCallId": 1, "input": "搜索 login 相关代码"}
  
  event: tool_call.completed
  data: {"toolCallId": 1, "output": "找到 3 个文件"}
  
  event: step.completed
  data: {"stepId": 1, "result": "..."

  event: approval.created
  data: {"approvalId": 1, "action": "需要审批：修改代码"}
  
  event: run.completed
  data: {"status": "SUCCEEDED", "summary": "执行完成"}
```

**与 WebSocket 的关系：**
- WebSocket：P0 IM 的常驻双向连接，用于消息实时收发
- SSE：P1 Agent 执行的单向日志流，一个用户观察一个执行过程

两者互补，不冲突。
