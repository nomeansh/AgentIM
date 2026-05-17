package com.AgentIM.business.im.service.impl;

import com.AgentIM.business.im.constant.ImEventType;
import com.AgentIM.business.im.domain.entity.ImChat;
import com.AgentIM.business.im.domain.entity.ImMessage;
import com.AgentIM.business.im.domain.entity.ImPinnedMessage;
import com.AgentIM.business.im.domain.vo.ImPinnedMessageVo;
import com.AgentIM.business.im.mapper.ImPinnedMessageMapper;
import com.AgentIM.business.im.permission.ImPermissionService;
import com.AgentIM.business.im.service.IImMessageService;
import com.AgentIM.business.im.service.IImPinnedMessageService;
import com.AgentIM.business.im.support.ImAuditService;
import com.AgentIM.business.im.support.ImEventPublisher;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.MapstructUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * 置顶消息服务实现。
 */
@Service
@RequiredArgsConstructor
public class ImPinnedMessageServiceImpl implements IImPinnedMessageService {

    private final ImPinnedMessageMapper pinnedMessageMapper;
    private final IImMessageService messageService;
    private final ImPermissionService permissionService;
    private final ImAuditService auditService;
    private final ImEventPublisher eventPublisher;

    /**
     * 置顶消息。
     *
     * <p>权限通过聊天类型和成员角色共同判断，重复置顶直接返回已有记录。</p>
     *
     * @param messageId 消息 ID
     * @return 置顶记录视图
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ImPinnedMessageVo pinMessage(Long messageId) {
        ImMessage message = messageService.requireMessage(messageId);
        ImChat chat = permissionService.requireReadable(message.getChatId());
        if (!permissionService.canPin(chat)) {
            throw new ServiceException("当前用户无权置顶消息");
        }
        Long actorId = permissionService.currentUserId();
        ImPinnedMessage pin = pinnedMessageMapper.selectOne(new LambdaQueryWrapper<ImPinnedMessage>()
            .eq(ImPinnedMessage::getChatId, message.getChatId())
            .eq(ImPinnedMessage::getMessageId, messageId)
            .last("LIMIT 1"));
        if (pin == null) {
            pin = new ImPinnedMessage();
            pin.setChatId(message.getChatId());
            pin.setMessageId(messageId);
            pin.setPinnedBy(actorId);
            pin.setPinnedTime(new Date());
            pin.setDelFlag("0");
            pinnedMessageMapper.insert(pin);
        }
        ImPinnedMessageVo vo = MapstructUtils.convert(pin, ImPinnedMessageVo.class);
        vo.setMessage(messageService.getMessageVo(messageId));
        auditService.record(message.getChatId(), "message", messageId, "PIN_MESSAGE", actorId,
            "置顶消息", pin);
        eventPublisher.publishToChat(ImEventType.MESSAGE_PINNED, message.getChatId(), actorId, vo);
        return vo;
    }

    /**
     * 取消置顶消息。
     *
     * <p>只有具备置顶权限的用户可以取消置顶。记录不存在时保持幂等。</p>
     *
     * @param messageId 消息 ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unpinMessage(Long messageId) {
        ImMessage message = messageService.requireMessage(messageId);
        ImChat chat = permissionService.requireReadable(message.getChatId());
        if (!permissionService.canPin(chat)) {
            throw new ServiceException("当前用户无权取消置顶");
        }
        ImPinnedMessage pin = pinnedMessageMapper.selectOne(new LambdaQueryWrapper<ImPinnedMessage>()
            .eq(ImPinnedMessage::getChatId, message.getChatId())
            .eq(ImPinnedMessage::getMessageId, messageId)
            .last("LIMIT 1"));
        if (pin == null) {
            return;
        }
        pinnedMessageMapper.deleteById(pin.getId());
        Long actorId = permissionService.currentUserId();
        auditService.record(message.getChatId(), "message", messageId, "UNPIN_MESSAGE", actorId,
            "取消置顶消息", pin);
        eventPublisher.publishToChat(ImEventType.MESSAGE_UNPINNED, message.getChatId(), actorId, pin);
    }

    /**
     * 查询聊天置顶消息。
     *
     * <p>置顶记录会补齐消息详情，方便客户端一次请求完成顶部置顶区渲染。</p>
     *
     * @param chatId 聊天 ID
     * @return 置顶消息列表
     */
    @Override
    public List<ImPinnedMessageVo> listPinned(Long chatId) {
        permissionService.requireReadable(chatId);
        List<ImPinnedMessageVo> list = pinnedMessageMapper.selectByChatId(chatId);
        list.forEach(item -> item.setMessage(messageService.getMessageVo(item.getMessageId())));
        return list;
    }
}
