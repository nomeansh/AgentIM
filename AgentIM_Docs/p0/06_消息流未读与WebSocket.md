# 消息流、未读与 WebSocket

## 概述

消息是 AgentIM 的生命线。本章详细描述一条消息从发送到出现在所有接收者屏幕上的完整旅程，以及背后的未读模型、多设备同步和 WebSocket 实时推送机制。

## 消息发送全流程

### 步骤拆解

一条消息从用户点击"发送"到出现在所有聊天成员的屏幕上，经历以下步骤：

```
┌─────────┐     ┌──────────────┐     ┌──────────────┐     ┌──────────────┐
│ 前端发送 │────>│ Controller   │────>│ Service      │────>│ 数据库写入   │
│ POST     │     │ 参数校验     │     │ 业务逻辑     │     │ im_message   │
└─────────┘     └──────────────┘     └──────────────┘     └──────┬───────┘
                                                                  │
                    ┌─────────────────────────────────────────────┘
                    ▼
         ┌──────────────────┐     ┌──────────────────┐     ┌──────────────────┐
         │ 事务提交后        │────>│ RocketMQ         │────>│ WebSocket 节点   │
         │ 领域事件发布      │     │ 广播 Topic      │     │ 按成员过滤推送   │
         └──────────────────┘     └──────────────────┘     └──────────────────┘
```

### 详细代码流程

以下是对应 `ImMessageServiceImpl.send()` 方法的完整业务逻辑。

#### 第一步：权限与参数校验

```java
// 1. 检查聊天是否存在，用户是否有写入权限
ImChat chat = permissionService.requireChat(chatId);
permissionService.checkChatWritable(chat.getId(), chat.getType());

// 2. 如果 type=channel，校验发送者是否有发言权限
if (chat.getType() == CHANNEL) {
    checkSenderIsAdminOrOwner(chat.getId());
}

// 3. 媒体消息（image/file/voice/video）必须有 resourceIds
if (isMediaType(bo.getMessageType()) && isEmpty(bo.getResourceIds())) {
    throw "媒体消息必须引用资源ID";
}

// 4. 如果引用回复，原消息必须存在且可读
if (bo.getReplyToMessageId() != null) {
    requireMessage(bo.getReplyToMessageId());
}

// 5. 检查引用的资源是否可读
checkResourcesReadable(chatId, bo.getResourceIds());
```

#### 第二步：幂等检查

```java
// 如果客户端传了 idempotentKey，检查是否已存在
if (idempotentKey 不为空) {
    ImMessage existing = findByChatAndKey(chatId, idempotentKey);
    if (existing != null) {
        return existing.getId();
    }
}
```

#### 第三步：构建消息对象

```java
// 确定 seq（聊天内递增序号）
Long nextSeq = chat.getSeq() + 1;

// 构建完整消息对象
ImMessage message = new ImMessage();
message.setChatId(chatId);
message.setSenderId(currentUserId());
message.setMessageType(messageType);      // text/image/file/voice/video/poll/system/forward
message.setContent(content);
message.setContentPayload(contentPayload); // 投票/位置等结构化数据
message.setResourceIds(toJson(resourceIds));
message.setReplyToMessageId(replyToMessageId);

// 转发信息（如果 type=forward）
if (isForward) {
    message.setForwardFromMessageId(forwardFromMessageId);
    message.setForwardFromChatId(forwardFromChatId);
    message.setForwardSenderId(forwardSenderId);
}

message.setClientMsgId(clientMsgId);
message.setIdempotentKey(idempotentKey);
message.setSeq(nextSeq);
message.setStatus("normal");
```

#### 第四步：持久化与副作用

```java
// 4a. 写入消息主表
baseMapper.insert(message);

// 4b. 如果是投票消息，写入 poll 和 poll_option
if (messageType == POLL) {
    pollService.createPoll(message.getId(), bo.getContentPayload());
}

// 4c. 更新聊天的最后消息状态
updateChatLastMessage(chatId, nextSeq, messageId, contentPreview);
// → 设置 chat.seq = nextSeq
// → 设置 chat.lastMsgId = messageId
// → 设置 chat.lastMsgContent = content 的前 100 字
// → 设置 chat.lastMsgTime = now

// 4d. 关联资源
if (有 resourceIds) {
    updateResources(chatId, messageId, resourceIds);
}

// 4e. 写入发送者的已读游标
upsertReadState(chatId, messageId, currentUserId());

// 4f. 发布领域事件（事务提交后执行）
publishAfterCommit("message.created", chatId, message);
```

#### 第五步：异步推送（事务提交后）

```java
// 事务提交后，TransactionSynchronization 触发
// 1. 构建 WebSocket 事件（含 eventId、eventType、payload）
// 2. 发送到 RocketMQ Topic: "im-event"
// 3. 所有 WebSocket 节点消费该 Topic
// 4. 每个节点查询该聊天的成员列表
// 5. 按 session 过滤出在线的聊天成员
// 6. 推送给每个成员的 WebSocket 连接
```

### 消息编辑

```java
// 编辑限制：
// 1. 只有消息发送者可以编辑
// 2. 48 小时内可编辑（可通过配置调整）
// 3. 可以更新：content、contentPayload、resourceIds
// 4. 不能更新：messageType、senderId、chatId
// 5. 编辑后 status → "edited"，记录 editTime
```

### 消息删除

```java
// 两种删除语义：

// 对所有人删除（撤回）：
// 1. 只有消息发送者或管理员可以操作
// 2. 48 小时内可撤回
// 3. status → "deleted_all"
// 4. 所有人不可见，保留占位 "[消息已撤回]"

// 仅自己删除（不修改消息主表）：
// 1. 任何聊天成员可以操作
// 2. 无时间限制
// 3. 向 im_user_message_hide 表插入 (user_id, message_id)
// 4. 查询消息列表时 LEFT JOIN 排除当前用户隐藏的消息
// 5. 仅自己不可见，其他人仍然正常看到
```

### 引用回复

```java
// 引用回复通过 replyToMessageId 字段实现：
// 1. 发消息时设置 replyToMessageId = 被回复的消息 ID
// 2. 服务端校验被回复的消息存在且可读
// 3. 前端展示时，在回复消息上方显示被回复消息的摘要
// 4. 点击引用摘要可跳转到原消息位置
```

### 转发

```java
// 转发通过 forwardFromMessageId 字段实现：
// 1. 发消息时设置 forwardFromMessageId = 来源消息 ID
// 2. 服务端记录转发来源（chatId、senderId）
// 3. 前端展示时显示 "转发自 @username" 头部
// 4. 可附带自己的评论（content 字段不为空时）
```

## 已读模型（飞书式）

### 设计选型：游标驱动，按人追踪

使用**游标模型**但扩展语义。底层仍然是每人每聊天一条已读游标，但上层查询时可以精确到"谁读了哪条消息"：

```
游标不只能算未读数——
  "用户 U 的 last_read_seq 是 12345"
  → 意味着 U 读到了该聊天内 seq=12345 的消息
  → 也意味着 U 读了该聊天内 seq 更小的所有消息
  → 对于任意消息 M: 如果 U 的 last_read_seq >= M.seq → U 已读 M
```

**为什么不需要按消息记录已读：**
- 游标天然保证连续性（读到了第 N 条 = 前 N-1 条都读了）
- "谁读了消息 M" 通过一次 JOIN 查询即可得出，不需要百万行状态表

### 私聊：三态对勾

| 图标 | 含义 | 判断条件 |
|------|------|----------|
| ✓ 单灰勾 | 已发送（消息已落库） | message 写入成功 |
| ✓✓ 双灰勾 | 已送达（接收方在线） | 接收方有活跃 WebSocket session |
| ✓✓ 双绿勾 | 已读 | 接收方 `last_read_seq >= message.seq` |

```
发送消息流程图：
  用户 A 发送 → message 落库 → ✓
  → WebSocket 推送给 B → B 在线 → ✓✓
  → B 打开聊天或滚动 → POST /read → A 收到事件 → ✓✓(绿)
```

### 群聊：已读人头 + 饼图

对于群聊中的任一条消息，前端可以展示：

```
┌─────────────────────────────────────────┐
│  张三：下午 3 点开会                      │
│                                         │
│  ●●●●●●○○○○ 6/9 已读                     │
│  已读：李四、王五、赵六、孙七、周八、吴九    │
└─────────────────────────────────────────┘
```

**计算方式：**
```sql
-- 查询消息 M 的已读状态（所有聊天成员）
SELECT cm.user_id,
       rs.last_read_message_id,
       rs.last_read_seq,
       CASE WHEN rs.last_read_seq >= m.seq THEN true ELSE false END AS has_read
FROM im_message m
JOIN im_chat_member cm
  ON cm.chat_id = m.chat_id
LEFT JOIN im_message_read_state rs
  ON rs.user_id = cm.user_id AND rs.chat_id = cm.chat_id
WHERE m.id = :messageId
  AND cm.user_id != :senderId;
```

**前端饼图渲染：**
- `readCount` = has_read=true 的成员数
- `totalCount` = 所有成员数（不含发送者）
- `ratio` = readCount / totalCount
- 绿色扇区角度 = ratio × 360°
- 灰色扇区角度 = (1 - ratio) × 360°

### 标记已读的业务逻辑

```
POST /im/messages/{messageId}/read

触发场景：
  - 用户打开聊天
  - 用户滚动聊天（看到的新消息自动标记已读）
  - 有新消息到达且用户正在查看聊天

服务端：
  1. upsert im_message_read_state（游标只前进不后退）
  2. 发布 read_state.updated 事件 {userId, chatId, lastReadMessageId, lastReadSeq}
  3. 其他在线成员收到事件
  4. 前端更新所有受影响消息的已读状态（饼图实时刷新）
```

### 特殊消息的已读事件

某些高关注度消息需要单独推送已读更新：

| 消息类型 | 行为 |
|----------|------|
| @提及消息 | 被提及者标记已读时，单独推送更新给发送者 |
| 投票消息 | 已读比例实时刷新饼图 |
| 系统消息 | 不需要已读追踪 |

### 未读数计算

```sql
SELECT COUNT(1) FROM im_message m
LEFT JOIN im_user_message_hide h
  ON h.message_id = m.id AND h.user_id = :currentUserId
WHERE m.chat_id = :chatId
  AND m.seq > :lastReadSeq
  AND m.status != 'deleted_all'
  AND h.user_id IS NULL;
```

### 多设备已读同步

```
场景：用户在手机上看完消息，PC 端应该同步更新

  1. 手机 POST /messages/{id}/read
  2. 服务端 upsert 游标
  3. 发布 read_state.updated 事件
  4. PC（同用户不同 session）收到事件
  5. PC 更新本地未读数 + 所有消息的对勾/饼图状态
```

### 大聊天优化

```
小聊天（< 1000 条/天）：
  → 直接查数据库，简单可靠

大聊天（> 1000 条/天）：
  → Redis 维护近实时计数器
  → Key: "im:unread:{chatId}:{userId}" → count
  → 新消息到达时，非发送者的未读数 +1
  → 标记已读时，计数器清零
  → 已读人头列表：缓存 30 秒，减少对 DB 的冲击
```

## WebSocket 实时推送

### 连接管理

```
客户端连接：
  ws://host/ws/im?token=xxx

服务端处理：
  1. Sa-Token 校验 token
  2. 校验通过 → 建立 WebSocket 连接
  3. 注册 session（绑定 userId ↔ sessionId）
  4. 记录多设备（同一用户可以有多个 session）
  5. 发布 user.online 事件

心跳：
  客户端每 30 秒发送 ping
  服务端 60 秒无 pong → 自动清理 session
  清理后发布 user.offline 事件
```

### 多设备在线状态

```java
// 用户 online 的判断：
// 该用户有至少一个活跃 WebSocket session
// 用户 offline 的判断：
// 该用户没有任何活跃 WebSocket session（所有设备都断开）

// 用户状态变更时发布事件：
// 其他用户可以通过订阅状态事件了解联系人在线状态
```

### 分布式会话

```
问题：WebSocket 服务可能部署多个节点
方案：RocketMQ 广播 Topic

节点 A 收到 message.created 事件（通过 RocketMQ 消费组）：
  1. 查询聊天的所有成员 userIds
  2. 检查每个 userId 是否在本节点有在线 session
  3. 推送本节点的在线连接

同时，通过 RocketMQ 广播 Topic 将事件派发到其他节点：
  节点 B、C 重复上述过程

注意：同一用户有多个 session（多设备）时，所有 session 都推送
```

### 事件推送的实现

```java
@Component
public class ImEventPublisher {

    // 推送给聊天所有成员
    public void publishToChat(String eventType, Long chatId, Long actorId, Object payload) {
        List<Long> userIds = chatMemberMapper.selectUserIdsByChatId(chatId);
        publishToUsers(eventType, chatId, actorId, userIds, payload);
    }

    // 推送给指定用户集合（含多设备支持）
    public void publishToUsers(String eventType, Long chatId, Long actorId,
                                Collection<Long> userIds, Object payload) {
        if (isEmpty(userIds)) return;

        Map<String, Object> event = new LinkedHashMap<>();
        event.put("eventId", "evt_" + UUID.randomUUID().toString().replace("-", ""));
        event.put("eventType", eventType);
        event.put("chatId", chatId);
        event.put("actorId", actorId);
        event.put("payload", payload);
        event.put("occurredAt", new Date());

        WebSocketMessageDto dto = new WebSocketMessageDto();
        dto.setSessionKeys(userIds.stream().distinct().toList());
        dto.setMessage(JsonUtils.toJsonString(event));

        publishAfterCommit(dto);
    }

    private void publishAfterCommit(WebSocketMessageDto dto) {
        TransactionSynchronizationManager.registerSynchronization(
            new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    // 通过 RocketMQ 发送到 im-event Topic
                    rocketMQTemplate.convertAndSend("im-event", dto);
                }
            }
        );
    }
}
```

### P0 事件清单

| 事件 | 触发场景 | 推送给谁 |
|------|----------|----------|
| `message.created` | 新消息发送 | 聊天所有成员（除发送者在事件中已有消息） |
| `message.edited` | 消息编辑 | 聊天所有成员 |
| `message.deleted` | 对所有人删除 | 聊天所有成员 |
| `message.hidden` | 仅自己隐藏 | 仅操作者（用于多设备同步 UI） |
| `reaction.created` | 添加 emoji | 聊天所有成员 |
| `reaction.deleted` | 删除 emoji | 聊天所有成员 |
| `message.pinned` | 置顶消息 | 聊天所有成员 |
| `message.unpinned` | 取消置顶 | 聊天所有成员 |
| `read_state.updated` | 标记已读 | 同用户的其他设备（多设备同步）+ 消息发送者（已读回执） |
| `typing` | 输入中 | 聊天的其他成员 |
| `chat.updated` | 聊天设置/成员变更 | 聊天所有成员 |
| `user.status` | 用户在线/离线 | 联系人和共同聊天成员 |

## 多设备同步策略

### 核心原则

1. **所有消息存储在云端**，设备只是客户端
2. 设备离线期间的消息，上线后通过游标分页拉取
3. 已读状态、置顶等操作在所有设备间同步
4. 设备之间没有优先级之分

### 同步场景

```
场景 1：手机在线，PC 上线
  → PC 建立 WebSocket 连接
  → 拉取所有聊天的最近消息
  → 接收实时推送

场景 2：手机离线 2 小时，PC 在线发了消息
  → 手机重新上线
  → 标记已读等操作自动同步
  → 消息通过历史记录拉取（游标分页）

场景 3：手机标记已读，PC 同步
  → 手机 POST /messages/{id}/read
  → 发布 read_state.updated 事件
  → PC 收到事件，更新本地未读数
```

## 离线推送

### 问题

当用户所有设备都离线时，WebSocket 无法送达消息。需要离线推送通知用户"有新消息"。

### 方案

```
消息发送 → RocketMQ "im-event" Topic
    → 消费者检查目标用户在线状态（Redis session）
    → 如果在线：直接 WebSocket 推送（已处理）
    → 如果离线：
        → 写入离线消息队列（用户维度）
        → 记录未读摘要（"3 条新消息来自 张三、李四"）
        → 调用第三方推送服务（APNs / FCM / 厂商推送）
```

### 第三方推送渠道（P0 预留接口）

```java
public interface PushProvider {
    void push(Long userId, PushMessage msg);
}

// P0 可先用控制台日志/Webhook 代替
// P1 再接入 APNs / FCM / 华为/小米推送
```

### 用户上线补拉

```
用户重新建立 WebSocket 连接后：
  → 发送 sync.request 事件
  → 后端查询离线期间的消息（游标后的新消息）
  → 批量推送给该用户的所有活跃 session
```

## SSE 使用场景（P1 预留）

WebSocket 适合 IM 的常驻双向连接。SSE 为 P1 Agent 运行时预留：

```
P0：WebSocket（双向、常驻）← 消息收发、在线状态
P1：SSE（单向、按需）← Agent 执行日志流（见 ../p1/07_Agent运行时与权限.md）
```

### RocketMQ 配置

```yaml
rocketmq:
  name-server: 127.0.0.1:9876
  producer:
    group: agentim-event-producer
  consumer:
    group: agentim-event-consumer

agentim:
  event:
    topic: im-event
    consumer-tag: websocket
```

## 性能与可靠性关注点

| 关注点 | 问题 | 方案 |
|--------|------|------|
| 大聊天消息分页 | 全表扫描慢 | `(chat_id, seq)` 索引 + 游标分页 |
| 未读数实时计算 | 大聊天计算成本高 | Redis 缓存 + DB 兜底 |
| WebSocket 多节点路由 | session 分布在不同节点 | RocketMQ 广播 + 本地 session 过滤 |
| 大文件消息 | 接口响应慢 | 消息只存 resourceId，文件走对象存储 |
| 消息重复发送 | 网络抖动 | idempotentKey + 唯一索引 |
| 多设备消息乱序 | 各设备时间不同 | 服务端 seq 保证单调递增 |
| 消息编辑时效 | 用户依赖旧内容 | 48 小时编辑窗口 + edited 标记 |
