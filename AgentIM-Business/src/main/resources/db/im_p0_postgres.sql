-- AgentIM P0 核心 IM PostgreSQL 16 初始化脚本
-- 来源：AgentIM_Docs/p0/* 设计文档与 AgentIM-Business / AgentIM-auth 当前源码。
--
-- 认证说明：
-- 1. Auth 服务的 /login、/register 通过 Dubbo 调用 Business 的 RemoteAuthAccountService。
-- 2. P0 内置登录账号数据落在 im_user；密码字段 password 存储 bcrypt$ 前缀摘要，并兼容 sha256$ 存量摘要。
-- 3. 默认客户端 agentim-web、默认角色 im_user、接口权限 im:* 由 Business 代码常量提供，不依赖 sys_user/sys_client。
-- 4. 本脚本不创建传统后台 sys_* 登录表；P0 IM 用户主体以 im_user 为准。
--
-- 约束说明：
-- 1. 业务使用雪花 ID，所有主键均为 BIGINT，由 MyBatis-Plus ASSIGN_ID 生成，不使用自增序列。
-- 2. RuoYi 通用审计字段与 BaseEntity 对齐：create_dept/create_by/create_time/update_by/update_time/del_flag。
-- 3. 为兼容逻辑删除和消息留存，本脚本不使用数据库外键；关系一致性由 Service 层权限和事务保证。

BEGIN;

SET search_path TO public;

CREATE TABLE IF NOT EXISTS im_user (
    id BIGINT PRIMARY KEY,
    username VARCHAR(50) NOT NULL,
    password VARCHAR(255) NOT NULL,
    nickname VARCHAR(50),
    avatar VARCHAR(500),
    bio VARCHAR(200),
    phone VARCHAR(20),
    email VARCHAR(100),
    status CHAR(1) NOT NULL DEFAULT '0',
    data_scope VARCHAR(30) NOT NULL DEFAULT 'standard',
    permission_tags JSONB NOT NULL DEFAULT '[]'::jsonb,
    access_policy JSONB NOT NULL DEFAULT '{}'::jsonb,
    create_dept BIGINT DEFAULT -1,
    create_by BIGINT DEFAULT -1,
    create_time TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP,
    update_by BIGINT DEFAULT -1,
    update_time TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP,
    del_flag CHAR(1) NOT NULL DEFAULT '0',
    CONSTRAINT ck_im_user_status CHECK (status IN ('0', '1')),
    CONSTRAINT ck_im_user_data_scope CHECK (data_scope IN ('standard', 'restricted', 'trusted', 'blocked')),
    CONSTRAINT ck_im_user_permission_tags_json CHECK (jsonb_typeof(permission_tags) = 'array'),
    CONSTRAINT ck_im_user_access_policy_json CHECK (jsonb_typeof(access_policy) = 'object'),
    CONSTRAINT ck_im_user_del_flag CHECK (del_flag IN ('0', '2'))
);

COMMENT ON TABLE im_user IS 'IM 用户资料与内置登录账号表';
COMMENT ON COLUMN im_user.id IS '用户 ID，雪花 ID；Auth 登录态中的 userId';
COMMENT ON COLUMN im_user.username IS '用户名 @handle，全局唯一，注册时会转小写';
COMMENT ON COLUMN im_user.password IS '登录密码摘要，新数据为 bcrypt$ 前缀，兼容 sha256$ 存量摘要';
COMMENT ON COLUMN im_user.status IS '用户状态：0 正常，1 停用';
COMMENT ON COLUMN im_user.data_scope IS '用户级数据访问范围预留：standard 标准、restricted 受限、trusted 可信、blocked 阻断；用于非聊天资源策略扩展';
COMMENT ON COLUMN im_user.permission_tags IS '用户权限标签 JSONB 数组预留，用于后续业务资源按标签做 allow/deny 匹配';
COMMENT ON COLUMN im_user.access_policy IS '用户级数据访问策略 JSONB 对象预留，可放 allowTags、denyTags、resourceScopes、denyReason 等扩展配置';
COMMENT ON COLUMN im_user.del_flag IS '逻辑删除：0 正常，2 删除';

-- 兼容已经执行过旧版脚本的库：P0 初始版本只在聊天成员上做数据权限，
-- 这里为后续非聊天业务资源预留用户级策略字段，不改变现有聊天权限判断。
ALTER TABLE IF EXISTS im_user ADD COLUMN IF NOT EXISTS data_scope VARCHAR(30) NOT NULL DEFAULT 'standard';
ALTER TABLE IF EXISTS im_user ADD COLUMN IF NOT EXISTS permission_tags JSONB NOT NULL DEFAULT '[]'::jsonb;
ALTER TABLE IF EXISTS im_user ADD COLUMN IF NOT EXISTS access_policy JSONB NOT NULL DEFAULT '{}'::jsonb;
ALTER TABLE IF EXISTS im_user ALTER COLUMN data_scope SET DEFAULT 'standard';
ALTER TABLE IF EXISTS im_user ALTER COLUMN permission_tags SET DEFAULT '[]'::jsonb;
ALTER TABLE IF EXISTS im_user ALTER COLUMN access_policy SET DEFAULT '{}'::jsonb;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'ck_im_user_data_scope'
          AND conrelid = 'im_user'::regclass
    ) THEN
        ALTER TABLE im_user ADD CONSTRAINT ck_im_user_data_scope
            CHECK (data_scope IN ('standard', 'restricted', 'trusted', 'blocked'));
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'ck_im_user_permission_tags_json'
          AND conrelid = 'im_user'::regclass
    ) THEN
        ALTER TABLE im_user ADD CONSTRAINT ck_im_user_permission_tags_json
            CHECK (jsonb_typeof(permission_tags) = 'array');
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'ck_im_user_access_policy_json'
          AND conrelid = 'im_user'::regclass
    ) THEN
        ALTER TABLE im_user ADD CONSTRAINT ck_im_user_access_policy_json
            CHECK (jsonb_typeof(access_policy) = 'object');
    END IF;
END $$;

CREATE UNIQUE INDEX IF NOT EXISTS uk_im_user_username
    ON im_user (username)
    WHERE del_flag = '0';
CREATE INDEX IF NOT EXISTS idx_im_user_search
    ON im_user (username, nickname)
    WHERE del_flag = '0' AND status = '0';
CREATE INDEX IF NOT EXISTS idx_im_user_data_scope
    ON im_user (data_scope)
    WHERE del_flag = '0' AND status = '0';
CREATE INDEX IF NOT EXISTS idx_im_user_permission_tags_gin
    ON im_user USING GIN (permission_tags)
    WHERE del_flag = '0' AND status = '0';
CREATE INDEX IF NOT EXISTS idx_im_user_access_policy_gin
    ON im_user USING GIN (access_policy jsonb_path_ops)
    WHERE del_flag = '0' AND status = '0';

CREATE TABLE IF NOT EXISTS im_user_contact (
    id BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    contact_user_id BIGINT NOT NULL,
    remark VARCHAR(100),
    status CHAR(1) NOT NULL DEFAULT '0',
    create_dept BIGINT DEFAULT -1,
    create_by BIGINT DEFAULT -1,
    create_time TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP,
    update_by BIGINT DEFAULT -1,
    update_time TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP,
    del_flag CHAR(1) NOT NULL DEFAULT '0',
    CONSTRAINT ck_im_user_contact_status CHECK (status IN ('0', '1')),
    CONSTRAINT ck_im_user_contact_del_flag CHECK (del_flag IN ('0', '2')),
    CONSTRAINT ck_im_user_contact_not_self CHECK (user_id <> contact_user_id)
);

COMMENT ON TABLE im_user_contact IS 'IM 单向联系人关系表';
COMMENT ON COLUMN im_user_contact.user_id IS '联系人拥有者用户 ID';
COMMENT ON COLUMN im_user_contact.contact_user_id IS '被加入通讯录的用户 ID';
COMMENT ON COLUMN im_user_contact.status IS '关系状态：0 正常，1 已移除';

CREATE UNIQUE INDEX IF NOT EXISTS uk_im_contact
    ON im_user_contact (user_id, contact_user_id)
    WHERE del_flag = '0';
CREATE INDEX IF NOT EXISTS idx_im_contact_contact_user
    ON im_user_contact (contact_user_id)
    WHERE del_flag = '0';

CREATE TABLE IF NOT EXISTS im_chat (
    id BIGINT PRIMARY KEY,
    type VARCHAR(20) NOT NULL,
    title VARCHAR(100),
    avatar VARCHAR(500),
    description VARCHAR(500),
    owner_id BIGINT,
    seq BIGINT NOT NULL DEFAULT 0,
    last_msg_id BIGINT,
    last_msg_content VARCHAR(200),
    last_msg_time TIMESTAMP(6),
    status VARCHAR(20) NOT NULL DEFAULT 'active',
    create_dept BIGINT DEFAULT -1,
    create_by BIGINT DEFAULT -1,
    create_time TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP,
    update_by BIGINT DEFAULT -1,
    update_time TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP,
    del_flag CHAR(1) NOT NULL DEFAULT '0',
    CONSTRAINT ck_im_chat_type CHECK (type IN ('private', 'group', 'channel', 'saved')),
    CONSTRAINT ck_im_chat_status CHECK (status IN ('active', 'archived', 'deleted')),
    CONSTRAINT ck_im_chat_seq CHECK (seq >= 0),
    CONSTRAINT ck_im_chat_del_flag CHECK (del_flag IN ('0', '2'))
);

COMMENT ON TABLE im_chat IS 'IM 聊天会话，统一承载私聊、群组、频道和保存的消息';
COMMENT ON COLUMN im_chat.type IS '聊天类型：private、group、channel、saved';
COMMENT ON COLUMN im_chat.owner_id IS '群组/频道/保存消息所有者；私聊可为空';
COMMENT ON COLUMN im_chat.seq IS '聊天内最新消息序号，发送消息时 UPDATE ... RETURNING 原子递增';
COMMENT ON COLUMN im_chat.status IS '会话状态：active、archived、deleted';

CREATE INDEX IF NOT EXISTS idx_im_chat_owner
    ON im_chat (owner_id)
    WHERE del_flag = '0';
CREATE INDEX IF NOT EXISTS idx_im_chat_status_last_time
    ON im_chat (status, last_msg_time DESC NULLS LAST, update_time DESC NULLS LAST)
    WHERE del_flag = '0';

CREATE TABLE IF NOT EXISTS im_chat_member (
    id BIGINT PRIMARY KEY,
    chat_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    role VARCHAR(20) NOT NULL,
    joined_time TIMESTAMP(6),
    muted CHAR(1) NOT NULL DEFAULT '0',
    create_dept BIGINT DEFAULT -1,
    create_by BIGINT DEFAULT -1,
    create_time TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP,
    update_by BIGINT DEFAULT -1,
    update_time TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP,
    del_flag CHAR(1) NOT NULL DEFAULT '0',
    CONSTRAINT ck_im_chat_member_role CHECK (role IN ('owner', 'admin', 'member', 'subscriber')),
    CONSTRAINT ck_im_chat_member_muted CHECK (muted IN ('0', '1')),
    CONSTRAINT ck_im_chat_member_del_flag CHECK (del_flag IN ('0', '2'))
);

COMMENT ON TABLE im_chat_member IS 'IM 聊天成员与聊天级数据权限表';
COMMENT ON COLUMN im_chat_member.role IS '成员角色：owner、admin、member、subscriber';
COMMENT ON COLUMN im_chat_member.muted IS '是否免打扰：0 正常，1 免打扰';

CREATE UNIQUE INDEX IF NOT EXISTS uk_im_chat_user
    ON im_chat_member (chat_id, user_id)
    WHERE del_flag = '0';
CREATE INDEX IF NOT EXISTS idx_im_chat_member_user
    ON im_chat_member (user_id, chat_id)
    WHERE del_flag = '0';
CREATE INDEX IF NOT EXISTS idx_im_chat_member_chat_role
    ON im_chat_member (chat_id, role)
    WHERE del_flag = '0';

CREATE TABLE IF NOT EXISTS im_message (
    id BIGINT PRIMARY KEY,
    chat_id BIGINT NOT NULL,
    sender_id BIGINT,
    message_type VARCHAR(30) NOT NULL,
    content TEXT,
    content_payload JSONB,
    resource_ids JSONB,
    reply_to_message_id BIGINT,
    forward_from_message_id BIGINT,
    forward_from_chat_id BIGINT,
    forward_sender_id BIGINT,
    client_msg_id VARCHAR(100),
    idempotent_key VARCHAR(120) NOT NULL,
    seq BIGINT NOT NULL,
    reply_count INTEGER NOT NULL DEFAULT 0,
    status VARCHAR(30) NOT NULL DEFAULT 'normal',
    edit_time TIMESTAMP(6),
    delete_time TIMESTAMP(6),
    create_dept BIGINT DEFAULT -1,
    create_by BIGINT DEFAULT -1,
    create_time TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP,
    update_by BIGINT DEFAULT -1,
    update_time TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP,
    del_flag CHAR(1) NOT NULL DEFAULT '0',
    CONSTRAINT ck_im_message_type CHECK (message_type IN ('text', 'image', 'file', 'voice', 'video', 'poll', 'system', 'forward')),
    CONSTRAINT ck_im_message_status CHECK (status IN ('normal', 'edited', 'deleted_all')),
    CONSTRAINT ck_im_message_seq CHECK (seq > 0),
    CONSTRAINT ck_im_message_reply_count CHECK (reply_count >= 0),
    CONSTRAINT ck_im_message_del_flag CHECK (del_flag IN ('0', '2')),
    CONSTRAINT ck_im_message_resource_ids_json CHECK (resource_ids IS NULL OR jsonb_typeof(resource_ids) = 'array')
);

-- 兼容已经执行过旧版脚本的库：幂等建表语句遇到旧表会跳过建表，
-- 但后续 COMMENT/INDEX 和业务写入仍依赖这些列存在，因此这里先做幂等补列。
ALTER TABLE IF EXISTS im_message ADD COLUMN IF NOT EXISTS chat_id BIGINT;
ALTER TABLE IF EXISTS im_message ADD COLUMN IF NOT EXISTS sender_id BIGINT;
ALTER TABLE IF EXISTS im_message ADD COLUMN IF NOT EXISTS message_type VARCHAR(30);
ALTER TABLE IF EXISTS im_message ADD COLUMN IF NOT EXISTS content TEXT;
ALTER TABLE IF EXISTS im_message ADD COLUMN IF NOT EXISTS content_payload JSONB;
ALTER TABLE IF EXISTS im_message ADD COLUMN IF NOT EXISTS resource_ids JSONB;
ALTER TABLE IF EXISTS im_message ADD COLUMN IF NOT EXISTS reply_to_message_id BIGINT;
ALTER TABLE IF EXISTS im_message ADD COLUMN IF NOT EXISTS forward_from_message_id BIGINT;
ALTER TABLE IF EXISTS im_message ADD COLUMN IF NOT EXISTS forward_from_chat_id BIGINT;
ALTER TABLE IF EXISTS im_message ADD COLUMN IF NOT EXISTS forward_sender_id BIGINT;
ALTER TABLE IF EXISTS im_message ADD COLUMN IF NOT EXISTS client_msg_id VARCHAR(100);
ALTER TABLE IF EXISTS im_message ADD COLUMN IF NOT EXISTS idempotent_key VARCHAR(120);
ALTER TABLE IF EXISTS im_message ADD COLUMN IF NOT EXISTS seq BIGINT;
ALTER TABLE IF EXISTS im_message ADD COLUMN IF NOT EXISTS reply_count INTEGER DEFAULT 0;
ALTER TABLE IF EXISTS im_message ADD COLUMN IF NOT EXISTS status VARCHAR(30) DEFAULT 'normal';
ALTER TABLE IF EXISTS im_message ADD COLUMN IF NOT EXISTS edit_time TIMESTAMP(6);
ALTER TABLE IF EXISTS im_message ADD COLUMN IF NOT EXISTS delete_time TIMESTAMP(6);
ALTER TABLE IF EXISTS im_message ADD COLUMN IF NOT EXISTS create_dept BIGINT DEFAULT -1;
ALTER TABLE IF EXISTS im_message ADD COLUMN IF NOT EXISTS create_by BIGINT DEFAULT -1;
ALTER TABLE IF EXISTS im_message ADD COLUMN IF NOT EXISTS create_time TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE IF EXISTS im_message ADD COLUMN IF NOT EXISTS update_by BIGINT DEFAULT -1;
ALTER TABLE IF EXISTS im_message ADD COLUMN IF NOT EXISTS update_time TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE IF EXISTS im_message ADD COLUMN IF NOT EXISTS del_flag CHAR(1) DEFAULT '0';

COMMENT ON TABLE im_message IS 'IM 消息主表，富媒体只保存资源 ID 引用';
COMMENT ON COLUMN im_message.message_type IS '消息类型：text、image、file、voice、video、poll、system、forward';
COMMENT ON COLUMN im_message.content_payload IS '结构化 JSONB 载荷；Java 侧以 JSON 字符串读写';
COMMENT ON COLUMN im_message.resource_ids IS '资源 ID JSONB 数组；Java 侧以 JSON 字符串读写';
COMMENT ON COLUMN im_message.idempotent_key IS '客户端幂等键，同一聊天内唯一，发送消息必须传入';
COMMENT ON COLUMN im_message.seq IS '聊天内递增消息序号，用于游标分页和已读计算';
COMMENT ON COLUMN im_message.status IS '消息状态：normal、edited、deleted_all';

CREATE UNIQUE INDEX IF NOT EXISTS uk_im_message_idempotent
    ON im_message (chat_id, idempotent_key)
    WHERE del_flag = '0';
CREATE INDEX IF NOT EXISTS idx_im_message_chat_seq
    ON im_message (chat_id, seq DESC)
    WHERE del_flag = '0';
CREATE INDEX IF NOT EXISTS idx_im_message_visible_chat_seq
    ON im_message (chat_id, seq DESC)
    WHERE del_flag = '0' AND status <> 'deleted_all';
CREATE INDEX IF NOT EXISTS idx_im_message_chat_type
    ON im_message (chat_id, message_type)
    WHERE del_flag = '0';
CREATE INDEX IF NOT EXISTS idx_im_message_sender_time
    ON im_message (sender_id, create_time DESC)
    WHERE del_flag = '0';
CREATE INDEX IF NOT EXISTS idx_im_message_fts
    ON im_message USING GIN (to_tsvector('simple', COALESCE(content, '')))
    WHERE del_flag = '0' AND status IN ('normal', 'edited');

CREATE TABLE IF NOT EXISTS im_message_reply (
    id BIGINT PRIMARY KEY,
    message_id BIGINT NOT NULL,
    reply_to_message_id BIGINT NOT NULL,
    chat_id BIGINT NOT NULL,
    create_dept BIGINT DEFAULT -1,
    create_by BIGINT DEFAULT -1,
    create_time TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP,
    update_by BIGINT DEFAULT -1,
    update_time TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP,
    del_flag CHAR(1) NOT NULL DEFAULT '0',
    CONSTRAINT ck_im_message_reply_del_flag CHECK (del_flag IN ('0', '2')),
    CONSTRAINT ck_im_message_reply_not_self CHECK (message_id <> reply_to_message_id)
);

COMMENT ON TABLE im_message_reply IS 'IM 消息回复索引表，用于按原消息查询回复';

CREATE UNIQUE INDEX IF NOT EXISTS uk_im_message_reply_message
    ON im_message_reply (message_id)
    WHERE del_flag = '0';
CREATE INDEX IF NOT EXISTS idx_im_message_reply_target
    ON im_message_reply (reply_to_message_id, chat_id)
    WHERE del_flag = '0';

CREATE TABLE IF NOT EXISTS im_user_message_hide (
    id BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    message_id BIGINT NOT NULL,
    hidden_time TIMESTAMP(6),
    create_dept BIGINT DEFAULT -1,
    create_by BIGINT DEFAULT -1,
    create_time TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP,
    update_by BIGINT DEFAULT -1,
    update_time TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP,
    del_flag CHAR(1) NOT NULL DEFAULT '0',
    CONSTRAINT ck_im_user_message_hide_del_flag CHECK (del_flag IN ('0', '2'))
);

COMMENT ON TABLE im_user_message_hide IS 'IM 用户仅自己隐藏消息表';

CREATE UNIQUE INDEX IF NOT EXISTS uk_im_user_hide
    ON im_user_message_hide (user_id, message_id)
    WHERE del_flag = '0';
CREATE INDEX IF NOT EXISTS idx_im_user_hide_message
    ON im_user_message_hide (message_id)
    WHERE del_flag = '0';

CREATE TABLE IF NOT EXISTS im_message_reaction (
    id BIGINT PRIMARY KEY,
    message_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    reaction VARCHAR(50) NOT NULL,
    create_dept BIGINT DEFAULT -1,
    create_by BIGINT DEFAULT -1,
    create_time TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP,
    update_by BIGINT DEFAULT -1,
    update_time TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP,
    del_flag CHAR(1) NOT NULL DEFAULT '0',
    CONSTRAINT ck_im_message_reaction_del_flag CHECK (del_flag IN ('0', '2'))
);

COMMENT ON TABLE im_message_reaction IS 'IM 消息 Emoji 反应表';

CREATE UNIQUE INDEX IF NOT EXISTS uk_im_reaction
    ON im_message_reaction (message_id, user_id, reaction)
    WHERE del_flag = '0';
CREATE INDEX IF NOT EXISTS idx_im_reaction_message
    ON im_message_reaction (message_id)
    WHERE del_flag = '0';

CREATE TABLE IF NOT EXISTS im_message_read_state (
    id BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    chat_id BIGINT NOT NULL,
    last_read_message_id BIGINT NOT NULL,
    last_read_seq BIGINT NOT NULL DEFAULT 0,
    last_read_time TIMESTAMP(6),
    create_dept BIGINT DEFAULT -1,
    create_by BIGINT DEFAULT -1,
    create_time TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP,
    update_by BIGINT DEFAULT -1,
    update_time TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP,
    del_flag CHAR(1) NOT NULL DEFAULT '0',
    CONSTRAINT ck_im_read_state_seq CHECK (last_read_seq >= 0),
    CONSTRAINT ck_im_read_state_del_flag CHECK (del_flag IN ('0', '2'))
);

COMMENT ON TABLE im_message_read_state IS 'IM 已读游标表，每人每聊天一条记录';
COMMENT ON COLUMN im_message_read_state.last_read_seq IS '最后阅读到的聊天内序号，只能前进不后退';

CREATE UNIQUE INDEX IF NOT EXISTS uk_im_read_state_user_chat
    ON im_message_read_state (user_id, chat_id)
    WHERE del_flag = '0';
CREATE INDEX IF NOT EXISTS idx_im_read_state_chat_seq
    ON im_message_read_state (chat_id, last_read_seq)
    WHERE del_flag = '0';

CREATE TABLE IF NOT EXISTS im_pinned_message (
    id BIGINT PRIMARY KEY,
    chat_id BIGINT NOT NULL,
    message_id BIGINT NOT NULL,
    pinned_by BIGINT,
    pinned_time TIMESTAMP(6),
    create_dept BIGINT DEFAULT -1,
    create_by BIGINT DEFAULT -1,
    create_time TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP,
    update_by BIGINT DEFAULT -1,
    update_time TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP,
    del_flag CHAR(1) NOT NULL DEFAULT '0',
    CONSTRAINT ck_im_pinned_message_del_flag CHECK (del_flag IN ('0', '2'))
);

COMMENT ON TABLE im_pinned_message IS 'IM 聊天置顶消息表';

CREATE UNIQUE INDEX IF NOT EXISTS uk_im_pinned_chat_message
    ON im_pinned_message (chat_id, message_id)
    WHERE del_flag = '0';
CREATE INDEX IF NOT EXISTS idx_im_pinned_chat_time
    ON im_pinned_message (chat_id, pinned_time DESC)
    WHERE del_flag = '0';

CREATE TABLE IF NOT EXISTS im_resource (
    id BIGINT PRIMARY KEY,
    chat_id BIGINT,
    message_id BIGINT,
    uploader_id BIGINT NOT NULL,
    resource_type VARCHAR(30) NOT NULL,
    original_name VARCHAR(255),
    mime_type VARCHAR(100),
    size BIGINT,
    storage_provider VARCHAR(30),
    object_key VARCHAR(500) NOT NULL,
    thumbnail_key VARCHAR(500),
    width INTEGER,
    height INTEGER,
    duration INTEGER,
    access_level VARCHAR(20) NOT NULL DEFAULT 'chat',
    url VARCHAR(1000),
    create_dept BIGINT DEFAULT -1,
    create_by BIGINT DEFAULT -1,
    create_time TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP,
    update_by BIGINT DEFAULT -1,
    update_time TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP,
    del_flag CHAR(1) NOT NULL DEFAULT '0',
    CONSTRAINT ck_im_resource_type CHECK (resource_type IN ('file', 'image', 'voice', 'video', 'other')),
    CONSTRAINT ck_im_resource_access_level CHECK (access_level IN ('private', 'chat', 'public')),
    CONSTRAINT ck_im_resource_size CHECK (size IS NULL OR size >= 0),
    CONSTRAINT ck_im_resource_dimension CHECK (
        (width IS NULL OR width >= 0)
        AND (height IS NULL OR height >= 0)
        AND (duration IS NULL OR duration >= 0)
    ),
    CONSTRAINT ck_im_resource_del_flag CHECK (del_flag IN ('0', '2'))
);

COMMENT ON TABLE im_resource IS 'IM 文件/图片/语音/视频资源元信息表，不保存文件内容';
COMMENT ON COLUMN im_resource.object_key IS '对象存储键或远程文件标识';
COMMENT ON COLUMN im_resource.access_level IS '访问级别：private、chat、public';

CREATE INDEX IF NOT EXISTS idx_im_resource_chat
    ON im_resource (chat_id)
    WHERE del_flag = '0';
CREATE INDEX IF NOT EXISTS idx_im_resource_message
    ON im_resource (message_id)
    WHERE del_flag = '0';
CREATE INDEX IF NOT EXISTS idx_im_resource_uploader
    ON im_resource (uploader_id, create_time DESC)
    WHERE del_flag = '0';
CREATE INDEX IF NOT EXISTS idx_im_resource_bindable
    ON im_resource (uploader_id, chat_id, message_id)
    WHERE del_flag = '0';

CREATE TABLE IF NOT EXISTS im_poll (
    id BIGINT PRIMARY KEY,
    message_id BIGINT NOT NULL,
    question TEXT NOT NULL,
    multiple CHAR(1) NOT NULL DEFAULT '0',
    anonymous CHAR(1) NOT NULL DEFAULT '0',
    status VARCHAR(20) NOT NULL DEFAULT 'active',
    close_time TIMESTAMP(6),
    create_dept BIGINT DEFAULT -1,
    create_by BIGINT DEFAULT -1,
    create_time TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP,
    update_by BIGINT DEFAULT -1,
    update_time TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP,
    del_flag CHAR(1) NOT NULL DEFAULT '0',
    CONSTRAINT ck_im_poll_multiple CHECK (multiple IN ('0', '1')),
    CONSTRAINT ck_im_poll_anonymous CHECK (anonymous IN ('0', '1')),
    CONSTRAINT ck_im_poll_status CHECK (status IN ('active', 'closed')),
    CONSTRAINT ck_im_poll_del_flag CHECK (del_flag IN ('0', '2'))
);

COMMENT ON TABLE im_poll IS 'IM 投票主体表，与 poll 类型消息一一对应';

CREATE UNIQUE INDEX IF NOT EXISTS uk_im_poll_message
    ON im_poll (message_id)
    WHERE del_flag = '0';
CREATE INDEX IF NOT EXISTS idx_im_poll_status_close_time
    ON im_poll (status, close_time)
    WHERE del_flag = '0';

CREATE TABLE IF NOT EXISTS im_poll_option (
    id BIGINT PRIMARY KEY,
    poll_id BIGINT NOT NULL,
    text VARCHAR(200) NOT NULL,
    ordinal INTEGER NOT NULL,
    create_dept BIGINT DEFAULT -1,
    create_by BIGINT DEFAULT -1,
    create_time TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP,
    update_by BIGINT DEFAULT -1,
    update_time TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP,
    del_flag CHAR(1) NOT NULL DEFAULT '0',
    CONSTRAINT ck_im_poll_option_ordinal CHECK (ordinal > 0),
    CONSTRAINT ck_im_poll_option_del_flag CHECK (del_flag IN ('0', '2'))
);

COMMENT ON TABLE im_poll_option IS 'IM 投票选项表';

CREATE INDEX IF NOT EXISTS idx_im_poll_option_poll
    ON im_poll_option (poll_id, ordinal)
    WHERE del_flag = '0';

CREATE TABLE IF NOT EXISTS im_poll_vote (
    id BIGINT PRIMARY KEY,
    poll_id BIGINT NOT NULL,
    option_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    voted_time TIMESTAMP(6),
    create_dept BIGINT DEFAULT -1,
    create_by BIGINT DEFAULT -1,
    create_time TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP,
    update_by BIGINT DEFAULT -1,
    update_time TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP,
    del_flag CHAR(1) NOT NULL DEFAULT '0',
    CONSTRAINT ck_im_poll_vote_del_flag CHECK (del_flag IN ('0', '2'))
);

COMMENT ON TABLE im_poll_vote IS 'IM 投票记录表';

CREATE UNIQUE INDEX IF NOT EXISTS uk_im_poll_vote
    ON im_poll_vote (poll_id, user_id, option_id)
    WHERE del_flag = '0';
CREATE INDEX IF NOT EXISTS idx_im_poll_vote_option
    ON im_poll_vote (option_id)
    WHERE del_flag = '0';
CREATE INDEX IF NOT EXISTS idx_im_poll_vote_user
    ON im_poll_vote (user_id, voted_time DESC)
    WHERE del_flag = '0';

CREATE TABLE IF NOT EXISTS im_audit_log (
    id BIGINT PRIMARY KEY,
    chat_id BIGINT,
    resource_type VARCHAR(60),
    resource_id BIGINT,
    action VARCHAR(100) NOT NULL,
    actor_id BIGINT,
    summary VARCHAR(1000),
    ipaddr VARCHAR(128),
    user_agent VARCHAR(500),
    payload JSONB,
    occurred_time TIMESTAMP(6) NOT NULL,
    create_dept BIGINT DEFAULT -1,
    create_by BIGINT DEFAULT -1,
    create_time TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP,
    update_by BIGINT DEFAULT -1,
    update_time TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP,
    del_flag CHAR(1) NOT NULL DEFAULT '0',
    CONSTRAINT ck_im_audit_log_del_flag CHECK (del_flag IN ('0', '2'))
);

COMMENT ON TABLE im_audit_log IS 'IM 关键写操作审计日志表';
COMMENT ON COLUMN im_audit_log.payload IS '审计详情 JSONB 载荷；Java 侧以 JSON 字符串写入';
COMMENT ON COLUMN im_audit_log.occurred_time IS '业务操作发生时间';

CREATE INDEX IF NOT EXISTS idx_im_audit_chat_time
    ON im_audit_log (chat_id, occurred_time DESC)
    WHERE del_flag = '0';
CREATE INDEX IF NOT EXISTS idx_im_audit_resource
    ON im_audit_log (resource_type, resource_id)
    WHERE del_flag = '0';
CREATE INDEX IF NOT EXISTS idx_im_audit_actor_time
    ON im_audit_log (actor_id, occurred_time DESC)
    WHERE del_flag = '0';
CREATE INDEX IF NOT EXISTS idx_im_audit_action_time
    ON im_audit_log (action, occurred_time DESC)
    WHERE del_flag = '0';

COMMIT;
