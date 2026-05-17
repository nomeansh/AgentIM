package com.AgentIM.business.im.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.mybatis.core.domain.BaseEntity;

import java.util.Date;

/**
 * IM 审计日志实体。
 *
 * <p>审计日志只追加关键操作，不参与普通业务删除。payload 以 JSON 字符串存储，用于保留修改前后
 * 摘要、成员变化、文件元信息等结构化证据。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("im_audit_log")
public class ImAuditLog extends BaseEntity {

    /**
     * 审计日志 ID。
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 关联聊天 ID。
     */
    private Long chatId;

    /**
     * 资源类型，如 message、chat、resource。
     */
    private String resourceType;

    /**
     * 资源 ID。
     */
    private Long resourceId;

    /**
     * 操作编码。
     */
    private String action;

    /**
     * 操作人用户 ID。
     */
    private Long actorId;

    /**
     * 人类可读摘要。
     */
    private String summary;

    /**
     * 客户端 IP。
     */
    private String ipaddr;

    /**
     * 客户端 User-Agent。
     */
    private String userAgent;

    /**
     * JSONB 详细载荷。
     */
    private String payload;

    /**
     * 发生时间。
     */
    private Date occurredTime;

    /**
     * 逻辑删除标记。
     */
    @TableLogic(value = "0", delval = "2")
    private String delFlag;
}
