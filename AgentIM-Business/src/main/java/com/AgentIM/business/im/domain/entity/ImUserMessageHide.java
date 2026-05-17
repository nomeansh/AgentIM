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
 * 用户隐藏消息实体。
 *
 * <p>仅自己删除消息时写入该表，不修改消息主表状态。消息列表查询通过 LEFT JOIN 排除此表中
 * 当前用户已经隐藏的消息。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("im_user_message_hide")
public class ImUserMessageHide extends BaseEntity {

    /**
     * 隐藏记录主键。
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 操作用户 ID。
     */
    private Long userId;

    /**
     * 被隐藏的消息 ID。
     */
    private Long messageId;

    /**
     * 隐藏时间。
     */
    private Date hiddenTime;

    /**
     * 逻辑删除标记。
     */
    @TableLogic(value = "0", delval = "2")
    private String delFlag;
}
