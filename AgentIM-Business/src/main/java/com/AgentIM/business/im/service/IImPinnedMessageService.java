package com.AgentIM.business.im.service;

import com.AgentIM.business.im.domain.vo.ImPinnedMessageVo;

import java.util.List;

/**
 * 置顶消息服务。
 */
public interface IImPinnedMessageService {

    /**
     * 置顶消息。
     *
     * <p>私聊双方可置顶；群组和频道要求 owner/admin 权限。重复置顶保持幂等。</p>
     *
     * @param messageId 消息 ID
     * @return 置顶记录视图
     */
    ImPinnedMessageVo pinMessage(Long messageId);

    /**
     * 取消置顶消息。
     *
     * <p>取消置顶会删除置顶记录并发布事务后事件，聊天消息本身不受影响。</p>
     *
     * @param messageId 消息 ID
     */
    void unpinMessage(Long messageId);

    /**
     * 查询聊天置顶消息。
     *
     * <p>调用前会校验当前用户是聊天成员，结果按置顶时间倒序排列。</p>
     *
     * @param chatId 聊天 ID
     * @return 置顶消息列表
     */
    List<ImPinnedMessageVo> listPinned(Long chatId);
}
