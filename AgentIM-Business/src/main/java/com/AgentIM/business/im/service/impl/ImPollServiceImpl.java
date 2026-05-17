package com.AgentIM.business.im.service.impl;

import com.AgentIM.business.im.domain.bo.ImPollVoteBo;
import com.AgentIM.business.im.domain.entity.ImMessage;
import com.AgentIM.business.im.domain.entity.ImPollVote;
import com.AgentIM.business.im.domain.vo.ImPollOptionVo;
import com.AgentIM.business.im.domain.vo.ImPollVo;
import com.AgentIM.business.im.enums.ImPollStatus;
import com.AgentIM.business.im.mapper.ImPollMapper;
import com.AgentIM.business.im.mapper.ImPollOptionMapper;
import com.AgentIM.business.im.mapper.ImPollVoteMapper;
import com.AgentIM.business.im.permission.ImPermissionService;
import com.AgentIM.business.im.service.IImMessageService;
import com.AgentIM.business.im.service.IImPollService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.exception.ServiceException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 投票服务实现。
 */
@Service
@RequiredArgsConstructor
public class ImPollServiceImpl implements IImPollService {

    private final ImPollMapper pollMapper;
    private final ImPollOptionMapper pollOptionMapper;
    private final ImPollVoteMapper pollVoteMapper;
    private final IImMessageService messageService;
    private final ImPermissionService permissionService;

    /**
     * 查询消息关联的投票详情。
     *
     * <p>返回对象包含选项票数和当前用户是否选择该选项。</p>
     *
     * @param messageId 消息 ID
     * @return 投票详情
     */
    @Override
    public ImPollVo getPoll(Long messageId) {
        ImMessage message = messageService.requireMessage(messageId);
        permissionService.requireReadable(message.getChatId());
        ImPollVo poll = pollMapper.selectVoByMessageId(messageId);
        if (poll == null) {
            throw new ServiceException("投票不存在");
        }
        poll.setOptions(pollOptionMapper.selectOptions(poll.getId(), permissionService.currentUserId()));
        return poll;
    }

    /**
     * 提交投票。
     *
     * <p>方法校验选项归属和投票状态。单选投票会先删除旧选择，再写入新选择；多选投票追加所选项
     * 并保持幂等。</p>
     *
     * @param messageId 消息 ID
     * @param bo 投票入参
     * @return 更新后的投票详情
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ImPollVo vote(Long messageId, ImPollVoteBo bo) {
        ImPollVo poll = getPoll(messageId);
        if (!ImPollStatus.ACTIVE.getCode().equals(poll.getStatus())) {
            throw new ServiceException("投票已关闭");
        }
        Set<Long> validOptionIds = poll.getOptions().stream().map(ImPollOptionVo::getId).collect(Collectors.toSet());
        if (!validOptionIds.containsAll(bo.getOptionIds())) {
            throw new ServiceException("投票选项不属于该投票");
        }
        if (!"1".equals(poll.getMultiple()) && bo.getOptionIds().size() != 1) {
            throw new ServiceException("单选投票只能选择一个选项");
        }
        Long userId = permissionService.currentUserId();
        if (!"1".equals(poll.getMultiple())) {
            pollVoteMapper.deleteUserVotes(poll.getId(), userId);
        }
        for (Long optionId : bo.getOptionIds()) {
            ImPollVote existing = pollVoteMapper.selectOne(new LambdaQueryWrapper<ImPollVote>()
                .eq(ImPollVote::getPollId, poll.getId())
                .eq(ImPollVote::getOptionId, optionId)
                .eq(ImPollVote::getUserId, userId)
                .last("LIMIT 1"));
            if (existing != null) {
                existing.setDelFlag("0");
                pollVoteMapper.updateById(existing);
                continue;
            }
            ImPollVote vote = new ImPollVote();
            vote.setPollId(poll.getId());
            vote.setOptionId(optionId);
            vote.setUserId(userId);
            vote.setVotedTime(new Date());
            vote.setDelFlag("0");
            pollVoteMapper.insert(vote);
        }
        return getPoll(messageId);
    }
}
