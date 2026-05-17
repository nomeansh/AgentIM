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
 * 消息实体。
 *
 * <p>消息是 P0 写入最频繁的核心实体。富媒体消息只保存资源 ID 引用，正文和结构化载荷分别放在
 * content 与 contentPayload 中，聊天内顺序由 seq 保证。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("im_message")
public class ImMessage extends BaseEntity {

    /**
     * 消息 ID。
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 所属聊天 ID。
     */
    private Long chatId;

    /**
     * 发送者用户 ID。
     */
    private Long senderId;

    /**
     * 消息类型。
     */
    private String messageType;

    /**
     * 消息正文或摘要。
     */
    private String content;

    /**
     * JSONB 结构化载荷，Java 侧以 JSON 字符串保存。
     */
    private String contentPayload;

    /**
     * JSONB 资源 ID 数组，Java 侧以 JSON 字符串保存。
     */
    private String resourceIds;

    /**
     * 被回复的原消息 ID。
     */
    private Long replyToMessageId;

    /**
     * 转发来源消息 ID。
     */
    private Long forwardFromMessageId;

    /**
     * 转发来源聊天 ID。
     */
    private Long forwardFromChatId;

    /**
     * 转发来源发送者 ID。
     */
    private Long forwardSenderId;

    /**
     * 客户端消息 ID。
     */
    private String clientMsgId;

    /**
     * 发送幂等键。
     */
    private String idempotentKey;

    /**
     * 聊天内递增消息序号。
     */
    private Long seq;

    /**
     * 回复数量。
     */
    private Integer replyCount;

    /**
     * 消息状态：normal、edited、deleted_all。
     */
    private String status;

    /**
     * 最后编辑时间。
     */
    private Date editTime;

    /**
     * 对所有人删除时间。
     */
    private Date deleteTime;

    /**
     * 逻辑删除标记。
     */
    @TableLogic(value = "0", delval = "2")
    private String delFlag;
}
