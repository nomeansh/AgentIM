package com.AgentIM.business.im.service;

import com.AgentIM.business.im.domain.bo.ImMessageEditBo;
import com.AgentIM.business.im.domain.bo.ImMessageSendBo;
import com.AgentIM.business.im.domain.entity.ImMessage;
import com.AgentIM.business.im.domain.vo.ImMessageVo;

import java.util.List;

/**
 * 消息服务。
 */
public interface IImMessageService {

    /**
     * 查询聊天消息列表。
     *
     * <p>消息列表使用 seq 游标分页，按倒序返回，并过滤当前用户隐藏和对所有人删除的消息。</p>
     *
     * @param chatId 聊天 ID
     * @param beforeSeq 历史游标，null 表示查询最新消息
     * @param limit 最大返回条数
     * @return 消息列表
     */
    List<ImMessageVo> listMessages(Long chatId, Long beforeSeq, int limit);

    /**
     * 发送消息。
     *
     * <p>发送流程包括成员写权限校验、媒体资源校验、幂等检查、seq 分配、消息落库、回复索引、
     * 投票创建、资源绑定、发送者已读游标更新和事务后事件发布。</p>
     *
     * @param chatId 聊天 ID
     * @param bo 发送消息入参
     * @return 新消息或幂等命中的既有消息视图
     */
    ImMessageVo sendMessage(Long chatId, ImMessageSendBo bo);

    /**
     * 编辑消息。
     *
     * <p>仅发送者可以在配置窗口期内编辑消息。编辑不会改变消息类型、发送者、聊天和 seq。</p>
     *
     * @param messageId 消息 ID
     * @param bo 编辑入参
     * @return 编辑后的消息视图
     */
    ImMessageVo editMessage(Long messageId, ImMessageEditBo bo);

    /**
     * 对所有人删除消息。
     *
     * <p>发送者或聊天管理员可以在配置窗口期内撤回消息。撤回只修改状态并保留消息占位。</p>
     *
     * @param messageId 消息 ID
     */
    void deleteForAll(Long messageId);

    /**
     * 仅当前用户隐藏消息。
     *
     * <p>该操作不修改消息主表，其他成员仍可见；事件只推送给当前用户的多设备会话。</p>
     *
     * @param messageId 消息 ID
     */
    void hideForSelf(Long messageId);

    /**
     * 根据 ID 获取消息并要求存在。
     *
     * <p>内部权限校验、已读、反应、置顶和投票流程复用该方法，统一过滤逻辑删除数据。</p>
     *
     * @param messageId 消息 ID
     * @return 消息实体
     */
    ImMessage requireMessage(Long messageId);

    /**
     * 根据 ID 查询消息视图。
     *
     * <p>该方法用于事件 payload、置顶详情和发送/编辑接口返回，返回对象会附带反应聚合信息。</p>
     *
     * @param messageId 消息 ID
     * @return 消息视图
     */
    ImMessageVo getMessageVo(Long messageId);
}
