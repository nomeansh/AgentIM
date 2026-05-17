package com.AgentIM.business.im.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.mybatis.core.domain.BaseEntity;

/**
 * 消息回复索引实体。
 *
 * <p>主消息表已经保存 replyToMessageId，该表用于高效查询某条消息的回复列表，并同步维护
 * replyCount。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("im_message_reply")
public class ImMessageReply extends BaseEntity {

    /**
     * 回复索引主键。
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 回复消息 ID。
     */
    private Long messageId;

    /**
     * 被回复的原消息 ID。
     */
    private Long replyToMessageId;

    /**
     * 聊天 ID。
     */
    private Long chatId;

    /**
     * 逻辑删除标记。
     */
    @TableLogic(value = "0", delval = "2")
    private String delFlag;
}
