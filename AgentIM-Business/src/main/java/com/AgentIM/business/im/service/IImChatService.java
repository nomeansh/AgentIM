package com.AgentIM.business.im.service;

import com.AgentIM.business.im.domain.bo.ImChatCreateBo;
import com.AgentIM.business.im.domain.bo.ImChatMemberBo;
import com.AgentIM.business.im.domain.bo.ImChatUpdateBo;
import com.AgentIM.business.im.domain.entity.ImChat;
import com.AgentIM.business.im.domain.vo.ImChatMemberVo;
import com.AgentIM.business.im.domain.vo.ImChatVo;

import java.util.List;

/**
 * 聊天会话服务。
 */
public interface IImChatService {

    /**
     * 查询当前用户的活跃聊天列表。
     *
     * <p>列表按最后消息时间倒序排列，并附带未读数。归档会话不在该列表中展示。</p>
     *
     * @return 聊天列表
     */
    List<ImChatVo> listChats();

    /**
     * 获取聊天详情。
     *
     * <p>该方法会校验当前用户是聊天成员，并返回成员列表，供客户端进入聊天或展示设置页时使用。</p>
     *
     * @param chatId 聊天 ID
     * @return 聊天详情
     */
    ImChatVo getChat(Long chatId);

    /**
     * 创建聊天。
     *
     * <p>私聊会优先复用已有会话；群组和频道会创建 owner 成员并写入审计日志；保存的消息不允许
     * 通过普通接口创建。</p>
     *
     * @param bo 创建聊天入参
     * @return 聊天详情
     */
    ImChatVo createChat(ImChatCreateBo bo);

    /**
     * 更新聊天设置。
     *
     * <p>只有 owner/admin 可以修改群组或频道设置。私聊和保存的消息不支持普通标题设置。</p>
     *
     * @param chatId 聊天 ID
     * @param bo 更新入参
     * @return 更新后的聊天详情
     */
    ImChatVo updateChat(Long chatId, ImChatUpdateBo bo);

    /**
     * 删除、退出或关闭聊天。
     *
     * <p>群组/频道 owner 可关闭会话；普通成员调用时移除自己的成员关系。私聊删除仅退出自己的
     * 会话视角，不物理删除消息。</p>
     *
     * @param chatId 聊天 ID
     */
    void deleteChat(Long chatId);

    /**
     * 归档聊天。
     *
     * <p>归档会将聊天状态改为 archived，使其不再出现在主列表中；消息仍然可访问。</p>
     *
     * @param chatId 聊天 ID
     */
    void archiveChat(Long chatId);

    /**
     * 取消归档聊天。
     *
     * <p>取消归档后聊天重新进入主列表。</p>
     *
     * @param chatId 聊天 ID
     */
    void unarchiveChat(Long chatId);

    /**
     * 查询聊天成员列表。
     *
     * <p>调用前会校验当前用户可读取该聊天，避免非成员枚举成员信息。</p>
     *
     * @param chatId 聊天 ID
     * @return 成员列表
     */
    List<ImChatMemberVo> listMembers(Long chatId);

    /**
     * 添加聊天成员。
     *
     * <p>私聊不允许添加成员；群组成员角色默认为 member；频道普通订阅者角色固定为 subscriber。</p>
     *
     * @param chatId 聊天 ID
     * @param bo 成员入参
     * @return 新增或恢复后的成员视图
     */
    ImChatMemberVo addMember(Long chatId, ImChatMemberBo bo);

    /**
     * 移除聊天成员。
     *
     * <p>owner/admin 可以移除其他成员，成员也可以移除自己来退出聊天；owner 不能直接移除自己以免
     * 群组或频道失去所有者。</p>
     *
     * @param chatId 聊天 ID
     * @param userId 被移除用户 ID
     */
    void removeMember(Long chatId, Long userId);

    /**
     * 修改成员角色。
     *
     * <p>仅 owner/admin 可调用。频道订阅者升级为 admin 后可发言和管理成员。</p>
     *
     * @param chatId 聊天 ID
     * @param userId 用户 ID
     * @param bo 成员角色入参
     * @return 修改后的成员视图
     */
    ImChatMemberVo updateMember(Long chatId, Long userId, ImChatMemberBo bo);

    /**
     * 获取或创建保存的消息聊天。
     *
     * <p>注册流程和保存的消息入口都会使用该方法，保证每个用户只有一个 saved 类型聊天。</p>
     *
     * @param userId 用户 ID
     * @return 保存的消息聊天实体
     */
    ImChat getOrCreateSavedChat(Long userId);

    /**
     * 获取或创建两个用户之间的私聊。
     *
     * <p>该方法保证私聊成员只有两人，若已存在私聊则直接返回，不重复创建。</p>
     *
     * @param userId1 用户一 ID
     * @param userId2 用户二 ID
     * @return 私聊实体
     */
    ImChat getOrCreatePrivateChat(Long userId1, Long userId2);
}
