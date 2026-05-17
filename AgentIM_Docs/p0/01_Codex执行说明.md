# Codex 执行说明

## 本文档的目标

本文档是 **Codex / AI 编码助手的首要参考**。当你需要在这个仓库中修改或新增代码时，请先阅读本文，理解：

1. P0 核心 IM 的完整业务闭环
2. 代码组织规则和模块边界
3. 编码规则和事务边界
4. 禁止事项

## P0 业务闭环：核心 IM

这是第一个可用的里程碑。从一个新用户注册到完成一次完整的沟通体验：

```
用户注册 → 设置用户名和个人资料
  → 搜索其他用户并添加联系人
  → 发起私聊 / 创建群组
  → 在聊天中发送文本、图片、文件、语音消息
  → 其他人可以回复、转发、加 emoji 反应
  → 消息通过 WebSocket 实时推送给在线成员
  → 可以编辑已发送的消息
  → 可以删除消息（对所有人或仅自己）
  → 可以置顶重要消息
  → 标记已读，未读数准确显示
  → 在多个设备上查看，消息同步
  → 搜索历史消息
  → 创建频道，用户订阅频道接收广播
  → 使用"保存的消息"作为云端便签
```

**P0 完成的判断标准：上述场景可以不借助任何后台页面完整走通。**

## 代码模块落位

```
AgentIM-Business/                          # 主业务模块
  src/main/java/com/AgentIM/business/
    im/                                    # P0：IM 核心业务
      user/                                #   用户资料、在线状态、联系人
      chat/                                #   聊天会话 CRUD（私聊/群组/频道）
      member/                              #   聊天成员管理
      message/                             #   消息发送/编辑/删除/回复/转发/置顶
      reaction/                            #   Emoji 反应
      readstate/                           #   已读游标
      resource/                            #   文件资源上传/下载/鉴权
      poll/                                #   投票
      search/                              #   全站/聊天内搜索
      pin/                                 #   置顶消息
      saved/                               #   保存的消息
      event/                               #   领域事件（WebSocket 发布）
      permission/                          #   权限校验服务
    common/                                # 公共能力
      enums/                               #   全部业务枚举

AgentIM-api/                               # Dubbo 远程接口
  AgentIM-api-auth/                        #   认证远程契约（LoginUser 等）
  AgentIM-api-resource/                    #   资源远程接口
  AgentIM-api-im/                          #   IM 远程接口（按需新增）

AgentIM-common/                            # 通用能力
  AgentIM-common-websocket/                #   WebSocket 底座
  AgentIM-common-sse/                      #   SSE 底座
  AgentIM-common-rocketmq/                 #   RocketMQ 事件广播
  AgentIM-common-redis/                    #   Redis 工具
  AgentIM-common-elasticsearch/            #   ES 搜索工具
```

## 编码规则

### 分层约定

遵循 RuoYi-Cloud-Plus 的 Controller → Service → Mapper 分层，每层职责明确：

```
Controller (REST 入口)
  → 参数校验、权限注解、调用 Service
  → 返回统一 R<T> 或 TableDataInfo（分页）

Service (业务逻辑)
  → 权限校验、事务管理、领域事件发布
  → 调用 Mapper 以及其它 Service

Mapper (数据访问)
  → MyBatis-Plus BaseMapper 扩展
  → 复杂查询写 XML，简单查询用 Lambda Wrapper

Domain (数据载体)
  Entity  → 数据库映射（ORM）
  Bo      → 业务入参（create/update）
  Vo      → 业务出参（response）
  DTO     → 跨服务传输（api 模块）
```

### 命名约束

| 规则 | 示例 |
|------|------|
| 表前缀 | `im_user`、`im_chat`、`im_message` |
| ID 策略 | 框架雪花 ID，不自增 |
| 权限标识格式 | `im:资源:动作` → `im:message:send` |
| 枚举值格式 | 小写 snake_case → `private_chat` |
| JSON 字段 | 消息卡片、提及列表、资源引用；不用于关系查询 |

### 事务边界

```
✅ 正确：Service 层开启事务，事务内只做数据库操作
❌ 错误：事务内调用外部 API、发送 HTTP 请求
✅ 正确：事务提交后通过领域事件触发异步操作
❌ 错误：在 Controller 层开启事务
```

## 禁止事项

这些都是实际开发中容易踩的坑，列在这里作为硬性约束：

| 禁止项 | 为什么 |
|--------|--------|
| 消息表直接存文件内容 | 文件进对象存储，消息只引用 resourceId |
| 跳过审计日志 | 所有关键操作需要可追溯 |
| DM/私聊成员超过 2 人 | 私聊必须只能有 2 个参与者 |
| 群组成员为 0 | 群组至少需要创建者 1 人 |
| 非成员发送消息 | 必须校验聊天成员关系 |
| 已删除消息仍然返回 | 列表查询必须过滤 deleted 状态 |
| 缺少幂等处理 | 消息发送必须用 idempotentKey 防重复 |

## 实现的推荐顺序

| 步骤 | 内容 | 依赖 |
|------|------|------|
| 1 | 数据库表创建 + 枚举类 + 权限标识定义 | 无 |
| 2 | 用户注册/登录（复用 RuoYi auth）+ 用户资料 | 步骤 1 |
| 3 | 用户在线状态 | 步骤 2 |
| 4 | 联系人管理（搜索用户、添加/删除联系人） | 步骤 2 |
| 5 | 聊天会话：私聊/群组/频道的 CRUD | 步骤 1 |
| 6 | 聊天成员管理 | 步骤 5 |
| 7 | 消息：发送、编辑、删除（对所有人/仅自己）| 步骤 5 |
| 8 | 消息扩展：引用回复、转发 | 步骤 7 |
| 9 | 消息扩展：Emoji 反应 | 步骤 7 |
| 10 | 已读游标 + 未读数 | 步骤 7 |
| 11 | 文件上传/下载 + 图片/语音/视频消息 | 步骤 7 |
| 12 | 置顶消息 | 步骤 7 |
| 13 | 保存的消息 | 步骤 7 |
| 14 | 投票 | 步骤 7 |
| 15 | WebSocket 事件推送 | 步骤 7-14 |
| 16 | 多设备同步 | 步骤 15 |
| 17 | 消息搜索 | 步骤 7 |
| 18 | 审计日志接入 | 步骤 2-17 |
