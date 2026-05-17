package com.AgentIM.business.im.permission;

import com.AgentIM.business.im.domain.entity.ImChat;
import com.AgentIM.business.im.domain.entity.ImMessage;
import com.AgentIM.business.im.enums.ImChatType;
import com.AgentIM.business.im.enums.ImMemberRole;
import com.AgentIM.business.im.mapper.ImChatMapper;
import com.AgentIM.business.im.mapper.ImChatMemberMapper;
import com.AgentIM.business.im.mapper.ImMessageMapper;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.satoken.utils.LoginHelper;
import org.springframework.stereotype.Service;

/**
 * IM 聊天级数据权限服务。
 *
 * <p>Sa-Token 注解负责接口级权限，本服务负责具体聊天、消息和成员关系的二次授权。所有 P0 业务
 * 写操作都应通过这里校验成员关系和角色。</p>
 */
@Service
@RequiredArgsConstructor
public class ImPermissionService {

    private final ImChatMapper chatMapper;
    private final ImChatMemberMapper chatMemberMapper;
    private final ImMessageMapper messageMapper;

    /**
     * 获取当前登录用户 ID 并要求存在。
     *
     * <p>该方法统一处理未登录或登录上下文异常情况，避免 Service 层散落空指针判断。</p>
     *
     * @return 当前用户 ID
     */
    public Long currentUserId() {
        Long userId = LoginHelper.getUserId();
        if (userId == null) {
            throw new ServiceException("用户未登录或登录态已失效");
        }
        return userId;
    }

    /**
     * 查询聊天并要求存在。
     *
     * <p>该方法只校验聊天本身存在，不校验当前用户是否是成员。读取和写入操作应继续调用
     * requireReadable 或 requireWritable。</p>
     *
     * @param chatId 聊天 ID
     * @return 聊天实体
     */
    public ImChat requireChat(Long chatId) {
        ImChat chat = chatMapper.selectById(chatId);
        if (chat == null || "deleted".equals(chat.getStatus())) {
            throw new ServiceException("聊天不存在或已删除");
        }
        return chat;
    }

    /**
     * 查询消息并要求存在。
     *
     * <p>该方法过滤逻辑删除数据，但不排除 deleted_all 状态，因为撤回事件、审计和部分管理操作仍
     * 需要读取占位消息。</p>
     *
     * @param messageId 消息 ID
     * @return 消息实体
     */
    public ImMessage requireMessage(Long messageId) {
        ImMessage message = messageMapper.selectById(messageId);
        if (message == null) {
            throw new ServiceException("消息不存在");
        }
        return message;
    }

    /**
     * 要求当前用户可以读取聊天。
     *
     * <p>P0 所有聊天内容只对成员可见。该方法校验聊天存在且当前用户有成员关系。</p>
     *
     * @param chatId 聊天 ID
     * @return 聊天实体
     */
    public ImChat requireReadable(Long chatId) {
        ImChat chat = requireChat(chatId);
        requireMember(chatId, currentUserId());
        return chat;
    }

    /**
     * 要求当前用户可以在聊天中发言。
     *
     * <p>私聊、群组和保存的消息要求有效成员关系；频道额外要求 owner/admin，subscriber 只可读不可
     * 发言。</p>
     *
     * @param chatId 聊天 ID
     * @return 聊天实体
     */
    public ImChat requireWritable(Long chatId) {
        ImChat chat = requireReadable(chatId);
        String role = roleOf(chatId, currentUserId());
        if (ImChatType.isChannel(chat.getType()) && !ImMemberRole.isManager(role)) {
            throw new ServiceException("频道普通订阅者不能发言");
        }
        return chat;
    }

    /**
     * 要求当前用户是聊天管理员。
     *
     * <p>owner/admin 才能管理成员、修改群组频道设置以及删除他人反应或消息。</p>
     *
     * @param chatId 聊天 ID
     */
    public void requireManager(Long chatId) {
        requireChat(chatId);
        String role = roleOf(chatId, currentUserId());
        if (!ImMemberRole.isManager(role)) {
            throw new ServiceException("当前用户没有聊天管理权限");
        }
    }

    /**
     * 要求指定用户是聊天成员。
     *
     * <p>成员关系不存在时抛出业务异常，调用方可以据此阻止非成员读取或操作聊天资源。</p>
     *
     * @param chatId 聊天 ID
     * @param userId 用户 ID
     */
    public void requireMember(Long chatId, Long userId) {
        if (roleOf(chatId, userId) == null) {
            throw new ServiceException("用户不是该聊天成员");
        }
    }

    /**
     * 查询用户在聊天中的角色。
     *
     * <p>返回 null 表示用户不是成员。该方法不抛异常，适合权限分支判断；强校验使用
     * requireMember。</p>
     *
     * @param chatId 聊天 ID
     * @param userId 用户 ID
     * @return 角色编码或 null
     */
    public String roleOf(Long chatId, Long userId) {
        return chatMemberMapper.selectRole(chatId, userId);
    }

    /**
     * 判断当前用户是否能删除指定消息。
     *
     * <p>发送者可以撤回自己的消息，聊天 owner/admin 可以删除任意成员消息。方法只做权限判断，
     * 时间窗口由消息服务处理。</p>
     *
     * @param message 消息实体
     * @return true 表示允许删除
     */
    public boolean canDeleteMessage(ImMessage message) {
        Long userId = currentUserId();
        if (userId.equals(message.getSenderId())) {
            return true;
        }
        return ImMemberRole.isManager(roleOf(message.getChatId(), userId));
    }

    /**
     * 判断当前用户是否可以置顶聊天消息。
     *
     * <p>私聊双方都可以置顶；群组和频道要求 owner/admin。调用前会校验当前用户是聊天成员。</p>
     *
     * @param chat 聊天实体
     * @return true 表示允许置顶
     */
    public boolean canPin(ImChat chat) {
        String role = roleOf(chat.getId(), currentUserId());
        if (ImChatType.PRIVATE.getCode().equals(chat.getType()) || ImChatType.SAVED.getCode().equals(chat.getType())) {
            return role != null;
        }
        return ImMemberRole.isManager(role);
    }
}
