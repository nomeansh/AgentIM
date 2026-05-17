package com.AgentIM.business.im.domain.bo;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 投票提交入参。
 *
 * <p>单选投票要求列表中只有一个选项，多选投票允许多个选项。服务层会校验选项都属于同一个
 * 投票，且当前用户可以读取对应消息。</p>
 */
@Data
public class ImPollVoteBo {

    /**
     * 被选择的投票选项 ID 列表。
     */
    @NotEmpty(message = "投票选项不能为空")
    private List<Long> optionIds;
}
