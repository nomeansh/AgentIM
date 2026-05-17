package com.AgentIM.business.im.service;

import com.AgentIM.business.im.domain.entity.ImMessage;
import com.AgentIM.business.im.domain.vo.ImMessageReadStatusVo;

/**
 * 已读游标服务。
 */
public interface IImReadStateService {

    /**
     * 标记当前用户读到指定消息。
     *
     * <p>游标只前进不后退。若客户端从旧设备提交较早消息，服务端会保留更大的 lastReadSeq。</p>
     *
     * @param messageId 消息 ID
     */
    void markRead(Long messageId);

    /**
     * 写入指定用户的已读游标。
     *
     * <p>消息发送后调用该方法将发送者自动标记为已读，也可供系统任务补齐游标。</p>
     *
     * @param userId 用户 ID
     * @param message 消息实体
     */
    void markRead(Long userId, ImMessage message);

    /**
     * 查询当前用户在聊天中的未读数。
     *
     * <p>未读数由消息 seq 与当前用户 lastReadSeq 比较得出，并排除隐藏和删除消息。</p>
     *
     * @param chatId 聊天 ID
     * @return 未读消息数
     */
    Long unreadCount(Long chatId);

    /**
     * 查询消息已读状态。
     *
     * <p>群聊返回 readBy/unreadBy 和比例；私聊同样使用成员游标计算，客户端可据此展示对勾状态。</p>
     *
     * @param messageId 消息 ID
     * @return 已读状态视图
     */
    ImMessageReadStatusVo getReadStatus(Long messageId);
}
