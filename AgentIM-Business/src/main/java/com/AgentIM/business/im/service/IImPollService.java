package com.AgentIM.business.im.service;

import com.AgentIM.business.im.domain.bo.ImPollVoteBo;
import com.AgentIM.business.im.domain.vo.ImPollVo;

/**
 * 投票服务。
 */
public interface IImPollService {

    /**
     * 查询消息关联的投票详情。
     *
     * <p>该方法会校验当前用户可读取消息所属聊天，并返回选项票数和当前用户选择状态。</p>
     *
     * @param messageId 消息 ID
     * @return 投票详情
     */
    ImPollVo getPoll(Long messageId);

    /**
     * 提交投票。
     *
     * <p>单选投票会替换旧选择，多选投票会保留多个选择。投票关闭后拒绝写入。</p>
     *
     * @param messageId 消息 ID
     * @param bo 投票入参
     * @return 更新后的投票详情
     */
    ImPollVo vote(Long messageId, ImPollVoteBo bo);
}
