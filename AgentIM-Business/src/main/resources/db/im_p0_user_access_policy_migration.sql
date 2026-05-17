-- AgentIM P0 用户级数据访问策略字段增量迁移脚本
-- 适用场景：数据库已经执行过旧版 im_p0_postgres.sql，但业务代码已经加入用户级数据策略预留字段。
--
-- 说明：
-- 1. 本脚本只补 im_user 上的用户级策略预留字段、约束和索引。
-- 2. P0 聊天数据权限仍由 im_chat_member 的成员关系和角色承担。
-- 3. 本脚本幂等，可重复执行。

BEGIN;

SET search_path TO public;

ALTER TABLE IF EXISTS im_user ADD COLUMN IF NOT EXISTS data_scope VARCHAR(30) NOT NULL DEFAULT 'standard';
ALTER TABLE IF EXISTS im_user ADD COLUMN IF NOT EXISTS permission_tags JSONB NOT NULL DEFAULT '[]'::jsonb;
ALTER TABLE IF EXISTS im_user ADD COLUMN IF NOT EXISTS access_policy JSONB NOT NULL DEFAULT '{}'::jsonb;

ALTER TABLE IF EXISTS im_user ALTER COLUMN data_scope SET DEFAULT 'standard';
ALTER TABLE IF EXISTS im_user ALTER COLUMN permission_tags SET DEFAULT '[]'::jsonb;
ALTER TABLE IF EXISTS im_user ALTER COLUMN access_policy SET DEFAULT '{}'::jsonb;

UPDATE im_user
SET data_scope = COALESCE(NULLIF(data_scope, ''), 'standard'),
    permission_tags = COALESCE(permission_tags, '[]'::jsonb),
    access_policy = COALESCE(access_policy, '{}'::jsonb)
WHERE data_scope IS NULL
   OR data_scope = ''
   OR permission_tags IS NULL
   OR access_policy IS NULL;

ALTER TABLE IF EXISTS im_user ALTER COLUMN data_scope SET NOT NULL;
ALTER TABLE IF EXISTS im_user ALTER COLUMN permission_tags SET NOT NULL;
ALTER TABLE IF EXISTS im_user ALTER COLUMN access_policy SET NOT NULL;

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

COMMENT ON COLUMN im_user.data_scope IS '用户级数据访问范围预留：standard 标准、restricted 受限、trusted 可信、blocked 阻断；用于非聊天资源策略扩展';
COMMENT ON COLUMN im_user.permission_tags IS '用户权限标签 JSONB 数组预留，用于后续业务资源按标签做 allow/deny 匹配';
COMMENT ON COLUMN im_user.access_policy IS '用户级数据访问策略 JSONB 对象预留，可放 allowTags、denyTags、resourceScopes、denyReason 等扩展配置';

CREATE INDEX IF NOT EXISTS idx_im_user_data_scope
    ON im_user (data_scope)
    WHERE del_flag = '0' AND status = '0';

CREATE INDEX IF NOT EXISTS idx_im_user_permission_tags_gin
    ON im_user USING GIN (permission_tags)
    WHERE del_flag = '0' AND status = '0';

CREATE INDEX IF NOT EXISTS idx_im_user_access_policy_gin
    ON im_user USING GIN (access_policy jsonb_path_ops)
    WHERE del_flag = '0' AND status = '0';

COMMIT;
