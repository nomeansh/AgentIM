package com.AgentIM.business.im.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.mybatis.core.domain.BaseEntity;

/**
 * 投票选项实体。
 *
 * <p>选项按 ordinal 稳定排序，客户端展示和投票提交都应使用 optionId，而不是依赖文本内容。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("im_poll_option")
public class ImPollOption extends BaseEntity {

    /**
     * 选项 ID。
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 投票 ID。
     */
    private Long pollId;

    /**
     * 选项文本。
     */
    private String text;

    /**
     * 排序号。
     */
    private Integer ordinal;

    /**
     * 逻辑删除标记。
     */
    @TableLogic(value = "0", delval = "2")
    private String delFlag;
}
