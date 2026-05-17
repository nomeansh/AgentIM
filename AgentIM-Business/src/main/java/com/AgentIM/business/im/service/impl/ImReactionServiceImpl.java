package com.AgentIM.business.im.service.impl;

import com.AgentIM.business.im.constant.ImEventType;
import com.AgentIM.business.im.domain.bo.ImReactionBo;
import com.AgentIM.business.im.domain.entity.ImMessage;
import com.AgentIM.business.im.domain.entity.ImMessageReaction;
import com.AgentIM.business.im.domain.vo.ImReactionVo;
import com.AgentIM.business.im.mapper.ImMessageReactionMapper;
import com.AgentIM.business.im.permission.ImPermissionService;
import com.AgentIM.business.im.service.IImMessageService;
import com.AgentIM.business.im.service.IImReactionService;
import com.AgentIM.business.im.support.ImEventPublisher;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.exception.ServiceException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 消息反应服务实现。
 */
@Service
@RequiredArgsConstructor
public class ImReactionServiceImpl implements IImReactionService {

    private final ImMessageReactionMapper reactionMapper;
    private final IImMessageService messageService;
    private final ImPermissionService permissionService;
    private final ImEventPublisher eventPublisher;

    /**
     * 查询消息反应明细。
     *
     * <p>当前用户必须能读取消息所属聊天。</p>
     *
     * @param messageId 消息 ID
     * @return 反应明细列表
     */
    @Override
    public List<ImReactionVo> listReactions(Long messageId) {
        ImMessage message = messageService.requireMessage(messageId);
        permissionService.requireReadable(message.getChatId());
        return reactionMapper.selectByMessageId(messageId);
    }

    /**
     * 添加 Emoji 反应。
     *
     * <p>重复添加会返回已有反应记录，不创建重复数据。</p>
     *
     * @param messageId 消息 ID
     * @param bo 反应入参
     * @return 反应视图
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ImReactionVo addReaction(Long messageId, ImReactionBo bo) {
        ImMessage message = messageService.requireMessage(messageId);
        permissionService.requireReadable(message.getChatId());
        Long userId = permissionService.currentUserId();
        ImMessageReaction reaction = reactionMapper.selectOne(new LambdaQueryWrapper<ImMessageReaction>()
            .eq(ImMessageReaction::getMessageId, messageId)
            .eq(ImMessageReaction::getUserId, userId)
            .eq(ImMessageReaction::getReaction, bo.getReaction())
            .last("LIMIT 1"));
        if (reaction == null) {
            reaction = new ImMessageReaction();
            reaction.setMessageId(messageId);
            reaction.setUserId(userId);
            reaction.setReaction(bo.getReaction());
            reaction.setDelFlag("0");
            reactionMapper.insert(reaction);
        }
        Long reactionId = reaction.getId();
        ImReactionVo vo = reactionMapper.selectByMessageId(messageId).stream()
            .filter(item -> reactionId.equals(item.getId()))
            .findFirst()
            .orElseThrow(() -> new ServiceException("反应创建失败"));
        eventPublisher.publishToChat(ImEventType.REACTION_CREATED, message.getChatId(), userId, vo);
        return vo;
    }

    /**
     * 删除 Emoji 反应。
     *
     * <p>普通成员只能删除自己的反应，聊天管理员可以删除其他人的反应。</p>
     *
     * @param messageId 消息 ID
     * @param userId 反应所属用户 ID
     * @param reaction 反应标识
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteReaction(Long messageId, Long userId, String reaction) {
        ImMessage message = messageService.requireMessage(messageId);
        permissionService.requireReadable(message.getChatId());
        Long actorId = permissionService.currentUserId();
        if (!actorId.equals(userId) && !com.AgentIM.business.im.enums.ImMemberRole.isManager(permissionService.roleOf(message.getChatId(), actorId))) {
            throw new ServiceException("无权删除他人的反应");
        }
        ImMessageReaction entity = reactionMapper.selectOne(new LambdaQueryWrapper<ImMessageReaction>()
            .eq(ImMessageReaction::getMessageId, messageId)
            .eq(ImMessageReaction::getUserId, userId)
            .eq(ImMessageReaction::getReaction, reaction)
            .last("LIMIT 1"));
        if (entity == null) {
            return;
        }
        reactionMapper.deleteById(entity.getId());
        eventPublisher.publishToChat(ImEventType.REACTION_DELETED, message.getChatId(), actorId, entity);
    }
}
