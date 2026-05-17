package com.AgentIM.business.im.service.impl;

import com.AgentIM.business.im.constant.ImEventType;
import com.AgentIM.business.im.domain.entity.ImMessage;
import com.AgentIM.business.im.domain.entity.ImMessageReadState;
import com.AgentIM.business.im.domain.vo.ImMessageReadStatusVo;
import com.AgentIM.business.im.domain.vo.ImReadMemberVo;
import com.AgentIM.business.im.mapper.ImChatMemberMapper;
import com.AgentIM.business.im.mapper.ImMessageMapper;
import com.AgentIM.business.im.mapper.ImMessageReadStateMapper;
import com.AgentIM.business.im.permission.ImPermissionService;
import com.AgentIM.business.im.service.IImReadStateService;
import com.AgentIM.business.im.support.ImEventPublisher;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.exception.ServiceException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * 已读游标服务实现。
 */
@Service
@RequiredArgsConstructor
public class ImReadStateServiceImpl implements IImReadStateService {

    private final ImMessageMapper messageMapper;
    private final ImMessageReadStateMapper readStateMapper;
    private final ImChatMemberMapper chatMemberMapper;
    private final ImPermissionService permissionService;
    private final ImEventPublisher eventPublisher;

    /**
     * 标记当前用户读到指定消息。
     *
     * <p>方法先校验消息存在和聊天可读，再把当前用户游标推进到该消息 seq。</p>
     *
     * @param messageId 消息 ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markRead(Long messageId) {
        ImMessage message = messageMapper.selectById(messageId);
        if (message == null) {
            throw new ServiceException("消息不存在");
        }
        permissionService.requireReadable(message.getChatId());
        markRead(permissionService.currentUserId(), message);
    }

    /**
     * 写入指定用户的已读游标。
     *
     * <p>游标不存在则创建；游标存在且新 seq 更大才更新。这样可以防止旧设备把新设备的已读进度
     * 覆盖回较小值。</p>
     *
     * @param userId 用户 ID
     * @param message 消息实体
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markRead(Long userId, ImMessage message) {
        ImMessageReadState state = readStateMapper.selectByUserAndChat(userId, message.getChatId());
        if (state == null) {
            state = new ImMessageReadState();
            state.setUserId(userId);
            state.setChatId(message.getChatId());
            state.setLastReadMessageId(message.getId());
            state.setLastReadSeq(message.getSeq());
            state.setLastReadTime(new Date());
            state.setDelFlag("0");
            readStateMapper.insert(state);
        } else if (state.getLastReadSeq() == null || state.getLastReadSeq() < message.getSeq()) {
            state.setLastReadMessageId(message.getId());
            state.setLastReadSeq(message.getSeq());
            state.setLastReadTime(new Date());
            readStateMapper.updateById(state);
        }
        eventPublisher.publishToChat(ImEventType.READ_STATE_UPDATED, message.getChatId(), userId, state);
    }

    /**
     * 查询当前用户在聊天中的未读数。
     *
     * <p>调用前校验当前用户是聊天成员，避免非成员推测聊天消息数量。</p>
     *
     * @param chatId 聊天 ID
     * @return 未读消息数
     */
    @Override
    public Long unreadCount(Long chatId) {
        permissionService.requireReadable(chatId);
        return messageMapper.countUnread(chatId, permissionService.currentUserId());
    }

    /**
     * 查询消息已读状态。
     *
     * <p>状态通过成员游标聚合得到，readBy 和 unreadBy 都不包含消息发送者本人。</p>
     *
     * @param messageId 消息 ID
     * @return 已读状态视图
     */
    @Override
    public ImMessageReadStatusVo getReadStatus(Long messageId) {
        ImMessage message = messageMapper.selectById(messageId);
        if (message == null) {
            throw new ServiceException("消息不存在");
        }
        permissionService.requireReadable(message.getChatId());
        List<ImReadMemberVo> states = chatMemberMapper.selectReadMembers(messageId);
        List<ImReadMemberVo> readBy = states.stream().filter(item -> Boolean.TRUE.equals(item.getHasRead())).toList();
        List<ImReadMemberVo> unreadBy = states.stream().filter(item -> !Boolean.TRUE.equals(item.getHasRead())).toList();
        ImMessageReadStatusVo vo = new ImMessageReadStatusVo();
        vo.setTotalCount(states.size());
        vo.setReadCount(readBy.size());
        vo.setRatio(states.isEmpty() ? 1D : (double) readBy.size() / states.size());
        vo.setReadBy(readBy);
        vo.setUnreadBy(unreadBy);
        vo.setStatus(readBy.isEmpty() ? "sent" : (unreadBy.isEmpty() ? "read" : "delivered"));
        return vo;
    }
}
