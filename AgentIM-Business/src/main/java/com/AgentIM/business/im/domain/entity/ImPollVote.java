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
 * 投票记录实体。
 *
 * <p>同一用户对同一投票选项只能投一次。单选投票会在 Service 层限制用户只能保留一个选项。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("im_poll_vote")
public class ImPollVote extends BaseEntity {

    /**
     * 投票记录 ID。
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 投票 ID。
     */
    private Long pollId;

    /**
     * 选项 ID。
     */
    private Long optionId;

    /**
     * 投票人用户 ID。
     */
    private Long userId;

    /**
     * 投票时间。
     */
    private Date votedTime;

    /**
     * 逻辑删除标记。
     */
    @TableLogic(value = "0", delval = "2")
    private String delFlag;
}
