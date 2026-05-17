# API 与数据契约

## 通用规则

### 响应格式

所有接口遵循 RuoYi-Cloud-Plus 的 `R<T>` 统一响应结构：

```json
// 成功
{ "code": 200, "msg": "操作成功", "data": {} }

// 失败
{ "code": 500, "msg": "聊天不存在", "data": null }

// 无权限
{ "code": 403, "msg": "无访问权限", "data": null }
```

### 分页接口

分页查询返回 `TableDataInfo` 结构：

```json
{
  "code": 200,
  "msg": "查询成功",
  "data": {
    "rows": [...],
    "total": 150,
    "page": 1,
    "pageSize": 20
  }
}
```

### 消息列表特殊说明

消息列表使用**游标分页**而非传统页码分页。这是因为消息是持续追加的流式数据：

- `beforeId` 或 `beforeSeq` 参数：查询比此 ID/序号更早的消息
- 按 `seq` 降序排列：最新的消息在前
- 每次返回 pageSize 条

## P0 API 详解

### 用户接口（/im/users）

用户资料和在线状态。

```
GET    /im/users/profile            ← 获取当前用户资料
PUT    /im/users/profile            ← 更新个人资料（昵称、头像、简介）
GET    /im/users/{userId}           ← 查看其他用户资料
GET    /im/users/{userId}/access-policy ← 查询用户级数据访问策略（预留）
PUT    /im/users/{userId}/access-policy ← 更新用户级数据访问策略（预留）
GET    /im/users/search?q=xxx       ← 搜索用户（按用户名/昵称）
```

**更新个人资料：**
```json
{
  "nickname": "张三",
  "bio": "全栈工程师",
  "avatar": "resourceId"
}
```

**更新用户级数据访问策略（预留接口，权限：`im:user:access-policy`）：**
```json
{
  "dataScope": "restricted",
  "permissionTags": "[\"p0-demo\", \"mobile-test\"]",
  "accessPolicy": "{\"allowTags\":[\"p0-demo\"],\"denyTags\":[\"admin-preview\"],\"resourceScopes\":[\"chat\"],\"denyReason\":\"演示受限账号\"}"
}
```

说明：该接口只写入 `im_user` 的用户级策略预留字段，面向后续非聊天业务资源。当前聊天消息、群组、频道、资源下载、已读和置顶仍通过 `im_chat_member` 成员关系与角色做数据级权限判断。

### 联系人接口（/im/contacts）

```
GET    /im/contacts                 ← 联系人列表
POST   /im/contacts                 ← 添加联系人
DELETE /im/contacts/{contactUserId} ← 删除联系人
```

**添加联系人：**
```json
{ "contactUserId": 1002 }
```

### 聊天接口（/im/chats）

聊天是统一概念：私聊、群组、频道、保存的消息都走这套接口。

#### 聊天 CRUD

```
GET    /im/chats                    ← 当前用户的聊天列表
GET    /im/chats/{chatId}           ← 聊天详情（含成员信息）
POST   /im/chats                    ← 创建聊天（群组/频道）
PUT    /im/chats/{chatId}           ← 更新聊天设置
DELETE /im/chats/{chatId}           ← 删除聊天/退出聊天
POST   /im/chats/{chatId}/archive   ← 归档聊天
POST   /im/chats/{chatId}/unarchive ← 取消归档
```

**创建群组：**
```json
{
  "type": "group",
  "title": "后端技术讨论",
  "description": "后端架构和技术方案讨论",
  "memberIds": [1002, 1003, 1004]
}
```

**创建频道：**
```json
{
  "type": "channel",
  "title": "产品公告",
  "description": "产品更新和版本发布通知"
}
```

**创建私聊（通过消息接口自动创建，也支持显式创建）：**
```
POST /im/chats
{
  "type": "private",
  "memberIds": [1002]
}
```

返回已有私聊或创建新的私聊。

**聊天列表过滤逻辑：**
- 返回当前用户参与的所有聊天（active 状态）
- 按 `last_msg_time` 降序排列（最新的在最前面）
- 每个聊天附带最后一条消息预览和未读数

#### 聊天成员管理

```
GET    /im/chats/{chatId}/members            ← 成员列表
POST   /im/chats/{chatId}/members            ← 添加成员/邀请
DELETE /im/chats/{chatId}/members/{userId}   ← 移除成员/退出
PUT    /im/chats/{chatId}/members/{userId}   ← 修改成员角色
```

**添加成员：**
```json
{ "userId": 1005, "role": "member" }
```

注意：
- `private` 聊天不能通过此接口管理成员（只有 2 个人）
- `channel` 添加用户的角色自动为 `subscriber`
- `group` 添加用户的角色可以为 `member` 或 `admin`

### 消息接口（/im）

消息是 AgentIM 中最核心、调用最频繁的接口组。

#### 消息 CRUD

```
GET    /im/chats/{chatId}/messages             ← 消息列表（游标分页）
POST   /im/chats/{chatId}/messages             ← 发送消息
PUT    /im/messages/{messageId}                ← 编辑消息
DELETE /im/messages/{messageId}                ← 删除消息
DELETE /im/messages/{messageId}/self           ← 仅自己删除
```

**发送文本消息：**
```json
{
  "messageType": "text",
  "content": "大家好，今天下午 3 点开会",
  "replyToMessageId": null,
  "clientMsgId": "c_xxxx",
  "idempotentKey": "unique_key_from_client"
}
```

**发送图片消息：**
```json
{
  "messageType": "image",
  "content": "看这个截图",
  "resourceIds": [1800000000000000001],
  "replyToMessageId": 1800000000000000002
}
```

**发送文件/语音/视频消息：**
```json
{
  "messageType": "file",
  "content": "Q2 技术方案",
  "resourceIds": [1800000000000000001]
}
```

**发送投票：**
```json
{
  "messageType": "poll",
  "content": "周五团建去哪里？",
  "contentPayload": {
    "multiple": false,
    "anonymous": false,
    "options": ["火锅", "烧烤", "日料"]
  }
}
```

**发送转发消息：**
```json
{
  "messageType": "forward",
  "content": "看看这条消息",
  "forwardFromMessageId": 1800000000000000001,
  "forwardFromChatId": 1800000000000000002
}
```

**发送消息的业务逻辑：**
```
1. 校验：当前用户是否有聊天的发送权限
2. 频道模式：如果 type=channel，校验用户是否有发言权限
3. 校验：图片/文件/语音/视频消息必须携带 resourceIds
4. 校验：引用的资源必须是当前用户可读的
5. 幂等检查：如果 idempotentKey 已存在，直接返回
6. 如果是引用回复，校验原消息存在且可读
7. 如果是转发，记录转发来源信息
8. 如果是投票，创建 poll/option 记录
9. 构建消息对象（设置 seq = 聊天当前 seq + 1）
10. 写入 im_message 表（以及关联的 poll/poll_option）
11. 更新聊天的 last_msg_id、last_msg_content、last_msg_time、seq
12. 关联 resourceIds 到 chatId/messageId
13. 写入发送者的已读游标
14. 发布 message.created 领域事件 → WebSocket 推送给聊天成员
```

#### 引用回复

```
POST /im/chats/{chatId}/messages
{
  "messageType": "text",
  "content": "同意！",
  "replyToMessageId": 1800000000000000001
}
```

**引用回复的展示逻辑：**
- 前端显示时，在被回复的消息下方缩进显示
- 点击引用消息可跳转到原消息位置
- 如果原消息已被删除，显示"[消息已删除]"

#### 转发消息

```
POST /im/chats/{chatId}/messages
{
  "messageType": "forward",
  "forwardFromMessageId": 1800000000000000001,
  "forwardFromChatId": 1800000000000000002
}
```

**转发展示逻辑：**
- 显示 "转发自 @username" 的头部标记
- 点击可跳转到原消息
- 可以附带自己的评论内容（同时发 `content` 字段）

#### 消息编辑

```
PUT /im/messages/{messageId}
{
  "content": "更新后的内容"
}
```

**编辑限制：**
- 只有消息发送者可以编辑
- 有时间限制（默认 48 小时内可编辑）
- 可以更新：content、contentPayload、resourceIds（追加/删除附件）
- 不能更新：messageType、senderId、senderType
- 编辑后 status → "edited"，记录 editTime

#### 消息删除

```
DELETE /im/messages/{messageId}                  ← 对所有人删除（撤回）
POST   /im/messages/{messageId}/hide             ← 仅自己隐藏
```

**对所有人删除（DELETE /messages/{id}）：**
- 只有消息发送者或管理员可以操作
- 有时间限制（默认 48 小时内）
- 消息对所有人不可见（status → deleted_all）
- 保留占位 "[消息已撤回]"

**仅自己隐藏（POST /messages/{id}/hide）：**
- 任何聊天成员可以操作
- 向 `im_user_message_hide` 表插入记录（不修改消息主表）
- 对自己不可见（查询时 LEFT JOIN 排除），对其他人仍然正常可见
- 没有时间限制

#### Emoji 反应

```
GET    /im/messages/{messageId}/reactions       ← 查询消息的所有反应
POST   /im/messages/{messageId}/reactions       ← 添加反应
DELETE /im/messages/{messageId}/reactions/{userId}/{reaction} ← 删除反应
```

**添加反应：**
```json
{ "reaction": "+1" }
```

业务逻辑：
- 同用户同消息同 reaction 幂等
- 用户只能删除自己的反应，管理员可以删除任何人的

#### 已读

```
POST   /im/messages/{messageId}/read            ← 标记已读（到某条消息）
GET    /im/chats/{chatId}/unread-count          ← 查询未读数
GET    /im/messages/{messageId}/read-status     ← 查询某条消息的已读状态（飞书式）
```

**标记已读的业务逻辑：**
```
1. 检查消息存在且用户有聊天读取权限
2. upsert im_message_read_state（游标只前进不后退）
3. 发布 read_state.updated 事件 → 其他成员 UI 实时更新饼图/对勾
```

**查询消息已读状态（GET /messages/{messageId}/read-status）：**

```json
// 请求
GET /im/messages/1800000000000000001/read-status

// 响应：群聊
{
  "totalCount": 10,          // 聊天成员总数（不含发送者）
  "readCount": 6,            // 已读人数
  "ratio": 0.6,              // 已读比例（绿色占比）
  "readBy": [
    { "userId": 1002, "nickname": "李四", "avatar": "xxx", "readTime": "2026-05-15T14:30:00" }
  ],
  "unreadBy": [
    { "userId": 1003, "nickname": "王五", "avatar": "xxx" },
    { "userId": 1004, "nickname": "赵六", "avatar": "xxx" }
  ]
}

// 响应：私聊
{
  "status": "read",          // "sent" | "delivered" | "read"
  "readTime": "2026-05-15T14:30:00"
}
```

**已读状态实时更新流程：**

```
用户 A 在群聊中打开聊天
  → 滚动到最新消息（messageId=12345）
  → 前端调用 POST /im/messages/12345/read
  → 服务端 upsert A 的游标
  → 发布 read_state.updated 事件 {userId: A, chatId: X, lastReadMessageId: 12345, lastReadSeq: 88}
  → 其他在线成员收到事件
  → 前端重新计算该聊天内所有消息的已读状态
  → 饼图实时刷新（绿色占比变化）
  → 已读人头/对勾图标更新
```

**前端饼图计算：**

```
对于某条消息 messageId=12340（群聊，10 个成员）：
  → 请求 GET /im/messages/12340/read-status（或从本地缓存计算）
  → readCount=6, totalCount=9（排除发送者）
  → ratio = 6/9 = 0.667
  → 饼图：绿色 240° / 灰色 120°
  → 底部显示 "6/9 已读"
```

#### 置顶

```
POST   /im/messages/{messageId}/pin             ← 置顶消息
DELETE /im/messages/{messageId}/pin             ← 取消置顶
GET    /im/chats/{chatId}/pinned                ← 获取置顶消息列表
```

**置顶业务逻辑：**
- 群组：owner/admin 可置顶
- 频道：管理员可置顶
- 私聊：双方可置顶
- 每个聊天可置顶多条消息

### 资源接口（/im/resources）

文件上传和下载。

```
POST   /im/resources/upload                ← 上传文件
GET    /im/resources/{resourceId}          ← 获取资源元信息
GET    /im/resources/{resourceId}/download ← 下载文件（需鉴权）
```

**文件上传流程：**
```
1. 前端以 multipart 形式上传文件
2. 后端接收文件 → 上传到 OSS/MinIO/S3
3. 图片自动生成缩略图
4. 创建 im_resource 记录
5. 返回 resourceId
6. 前端用 resourceId 发消息
```

**文件下载鉴权：**
```
1. 获取 resource 记录
2. 校验当前用户是否有对应聊天的访问权限
3. 生成临时下载 URL（可配置过期时间）
```

### 搜索接口（/im/search）

```
GET /im/search?q=xxx                  ← 全局搜索消息（ES）
GET /im/search?q=xxx&chatId={id}      ← 指定聊天内搜索（ES）
```

**搜索范围：**
- 消息内容（Elasticsearch 全文检索）
- 发送者信息

**实现方式：**
- 消息发送后通过 RocketMQ 异步同步到 ES
- 搜索请求直接查询 ES，不做数据库 LIKE

## WebSocket 事件

### 连接

```text
ws://host/ws/im?token=xxx
```

通过 Sa-Token 校验 token，连接建立后记录 userId ↔ sessionId 映射。
同一用户可以有多个 session（多设备）。

### P0 事件列表

| 事件类型 | 触发条件 | payload 说明 |
|----------|----------|-------------|
| `message.created` | 新消息发送 | 完整消息体 |
| `message.edited` | 消息编辑 | 更新后的消息体 |
| `message.deleted` | 对所有人删除 | messageId, chatId |
| `message.hidden` | 仅自己隐藏 | messageId, chatId, userId |
| `reaction.created` | 添加反应 | reaction 详情 |
| `reaction.deleted` | 删除反应 | reaction 详情 |
| `message.pinned` | 置顶消息 | pin 详情 |
| `message.unpinned` | 取消置顶 | pin 详情 |
| `read_state.updated` | 标记已读 | userId, chatId, lastReadMessageId, lastReadSeq |
| `chat.updated` | 聊天更新/成员变更 | chatId, 变更详情 |
| `user.status` | 用户在线/离线 | userId, status |
| `typing` | 输入中 | chatId, userId, userName |

### 事件推送机制

```
消息发送 → 领域事件 → TransactionSynchronization.afterCommit()
    → RocketMQ 广播到所有 WebSocket 节点
    → 各节点按聊天成员关系过滤
    → 推送给在线成员的 WebSocket 连接
```

**多设备推送：**
- 同一用户的一个 session 收到消息后，其他 session 也同步收到
- 标记已读在一个设备完成后，其他设备收到更新事件

## 保存的消息特殊说明

"保存的消息"是一个特殊的聊天类型（`type = saved`）：

```
获取保存的消息聊天：
  GET /im/chats/saved
  → 返回 type=saved 的聊天
  
发送消息到保存的消息：
  POST /im/chats/{savedChatId}/messages
  → 功能同普通消息发送
  → 可以用作云端便签、转发中转站
```

**自动创建：** 用户注册时由系统自动创建，不需要手动操作。
