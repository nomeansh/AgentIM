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
 * 投票实体。
 *
 * <p>投票与一条 poll 类型消息一一对应，messageId 同时是投票业务主键。投票选项和投票记录分别
 * 存放在 im_poll_option 与 im_poll_vote 中。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("im_poll")
public class ImPoll extends BaseEntity {

    /**
     * 投票主键。
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 关联消息 ID。
     */
    private Long messageId;

    /**
     * 投票问题。
     */
    private String question;

    /**
     * 是否多选，0 表示单选，1 表示多选。
     */
    private String multiple;

    /**
     * 是否匿名，0 表示记名，1 表示匿名。
     */
    private String anonymous;

    /**
     * 投票状态。
     */
    private String status;

    /**
     * 自动关闭时间。
     */
    private Date closeTime;

    /**
     * 逻辑删除标记。
     */
    @TableLogic(value = "0", delval = "2")
    private String delFlag;
}
