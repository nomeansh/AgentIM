package com.AgentIM.business.im.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.mybatis.core.domain.BaseEntity;

/**
 * 消息 Emoji 反应实体。
 *
 * <p>同一用户对同一消息的同一种反应通过唯一索引保持幂等。删除反应时用户只能删除自己的记录，
 * 管理员可删除任意成员的反应。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("im_message_reaction")
public class ImMessageReaction extends BaseEntity {

    /**
     * 反应记录主键。
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 消息 ID。
     */
    private Long messageId;

    /**
     * 用户 ID。
     */
    private Long userId;

    /**
     * Emoji 或反应标识。
     */
    private String reaction;

    /**
     * 逻辑删除标记。
     */
    @TableLogic(value = "0", delval = "2")
    private String delFlag;
}
