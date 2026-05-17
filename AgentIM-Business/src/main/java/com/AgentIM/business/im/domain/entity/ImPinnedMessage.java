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
 * 置顶消息实体。
 *
 * <p>每个聊天允许置顶多条消息，同一消息在同一聊天中只能置顶一次。取消置顶通过删除记录完成。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("im_pinned_message")
public class ImPinnedMessage extends BaseEntity {

    /**
     * 置顶记录主键。
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 聊天 ID。
     */
    private Long chatId;

    /**
     * 消息 ID。
     */
    private Long messageId;

    /**
     * 置顶操作人。
     */
    private Long pinnedBy;

    /**
     * 置顶时间。
     */
    private Date pinnedTime;

    /**
     * 逻辑删除标记。
     */
    @TableLogic(value = "0", delval = "2")
    private String delFlag;
}
