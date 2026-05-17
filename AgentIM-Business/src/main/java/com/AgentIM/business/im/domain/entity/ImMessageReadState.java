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
 * 消息已读游标实体。
 *
 * <p>每个用户在每个聊天中只有一条记录。lastReadSeq 只前进不后退，用于计算未读数以及飞书式
 * “谁读了某条消息”的状态。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("im_message_read_state")
public class ImMessageReadState extends BaseEntity {

    /**
     * 已读游标主键。
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 用户 ID。
     */
    private Long userId;

    /**
     * 聊天 ID。
     */
    private Long chatId;

    /**
     * 最后阅读到的消息 ID。
     */
    private Long lastReadMessageId;

    /**
     * 最后阅读到的聊天内序号。
     */
    private Long lastReadSeq;

    /**
     * 最后阅读时间。
     */
    private Date lastReadTime;

    /**
     * 逻辑删除标记。
     */
    @TableLogic(value = "0", delval = "2")
    private String delFlag;
}
