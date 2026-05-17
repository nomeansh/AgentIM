package com.AgentIM.business.im.service.impl;

import com.AgentIM.business.im.constant.ImConstants;
import com.AgentIM.business.im.constant.ImEventType;
import com.AgentIM.business.im.domain.bo.ImChatCreateBo;
import com.AgentIM.business.im.domain.bo.ImChatMemberBo;
import com.AgentIM.business.im.domain.bo.ImChatUpdateBo;
import com.AgentIM.business.im.domain.entity.ImChat;
import com.AgentIM.business.im.domain.entity.ImChatMember;
import com.AgentIM.business.im.domain.entity.ImUser;
import com.AgentIM.business.im.domain.vo.ImChatMemberVo;
import com.AgentIM.business.im.domain.vo.ImChatVo;
import com.AgentIM.business.im.enums.ImChatStatus;
import com.AgentIM.business.im.enums.ImChatType;
import com.AgentIM.business.im.enums.ImMemberRole;
import com.AgentIM.business.im.mapper.ImChatMapper;
import com.AgentIM.business.im.mapper.ImChatMemberMapper;
import com.AgentIM.business.im.mapper.ImUserMapper;
import com.AgentIM.business.im.permission.ImPermissionService;
import com.AgentIM.business.im.service.IImChatService;
import com.AgentIM.business.im.support.ImAuditService;
import com.AgentIM.business.im.support.ImEventPublisher;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 聊天会话服务实现。
 */
@Service
@RequiredArgsConstructor
public class ImChatServiceImpl implements IImChatService {

    private final ImChatMapper chatMapper;
    private final ImChatMemberMapper chatMemberMapper;
    private final ImUserMapper userMapper;
    private final ImPermissionService permissionService;
    private final ImAuditService auditService;
    private final ImEventPublisher eventPublisher;

    /**
     * 查询当前用户的活跃聊天列表。
     *
     * <p>未读数由 Mapper 内部基于已读游标计算，Service 仅负责读取当前用户上下文。</p>
     *
     * @return 聊天列表
     */
    @Override
    public List<ImChatVo> listChats() {
        return chatMapper.selectUserChats(permissionService.currentUserId());
    }

    /**
     * 获取聊天详情。
     *
     * <p>方法会校验当前用户是聊天成员，并附带成员列表。</p>
     *
     * @param chatId 聊天 ID
     * @return 聊天详情
     */
    @Override
    public ImChatVo getChat(Long chatId) {
        ImChat chat = permissionService.requireReadable(chatId);
        return buildChatSnapshot(chat);
    }

    /**
     * 创建聊天。
     *
     * <p>根据 type 分发到私聊、群组、频道创建逻辑。保存的消息由注册流程自动创建，不允许手动创建。</p>
     *
     * @param bo 创建聊天入参
     * @return 聊天详情
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ImChatVo createChat(ImChatCreateBo bo) {
        ImChatType type = ImChatType.require(bo.getType());
        Long currentUserId = permissionService.currentUserId();
        if (type == ImChatType.SAVED) {
            throw new ServiceException("保存的消息由系统自动创建");
        }
        ImChat chat;
        if (type == ImChatType.PRIVATE) {
            Long peerId = requireSinglePeer(bo.getMemberIds(), currentUserId);
            chat = getOrCreatePrivateChat(currentUserId, peerId);
        } else {
            chat = createManagedChat(type, bo, currentUserId);
        }
        eventPublisher.publishToChat(ImEventType.CHAT_CREATED, chat.getId(), currentUserId, buildChatSnapshot(chat));
        return getChat(chat.getId());
    }

    /**
     * 更新聊天设置。
     *
     * <p>只有群组和频道管理员可以修改展示字段；私聊和保存的消息设置保持系统默认。</p>
     *
     * @param chatId 聊天 ID
     * @param bo 更新入参
     * @return 更新后的聊天详情
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ImChatVo updateChat(Long chatId, ImChatUpdateBo bo) {
        ImChat chat = permissionService.requireChat(chatId);
        if (ImChatType.PRIVATE.getCode().equals(chat.getType()) || ImChatType.SAVED.getCode().equals(chat.getType())) {
            throw new ServiceException("该聊天类型不支持修改设置");
        }
        permissionService.requireManager(chatId);
        chat.setTitle(bo.getTitle());
        chat.setAvatar(bo.getAvatar());
        chat.setDescription(bo.getDescription());
        chatMapper.updateById(chat);
        Long actorId = permissionService.currentUserId();
        auditService.record(chatId, "chat", chatId, "UPDATE_CHAT", actorId, "更新聊天设置", bo);
        eventPublisher.publishToChat(ImEventType.CHAT_UPDATED, chatId, actorId, getChat(chatId));
        return getChat(chatId);
    }

    /**
     * 删除、退出或关闭聊天。
     *
     * <p>管理员会将群组或频道置为 deleted；普通成员会删除自己的成员关系，以退出聊天。</p>
     *
     * @param chatId 聊天 ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteChat(Long chatId) {
        ImChat chat = permissionService.requireReadable(chatId);
        Long actorId = permissionService.currentUserId();
        if (ImChatType.PRIVATE.getCode().equals(chat.getType())) {
            leavePrivateChat(chatId, actorId);
            return;
        }
        String role = permissionService.roleOf(chatId, actorId);
        if (ImMemberRole.isManager(role)) {
            chat.setStatus(ImChatStatus.DELETED.getCode());
            chatMapper.updateById(chat);
            auditService.record(chatId, "chat", chatId, "DELETE_CHAT", actorId, "删除聊天", chat);
            eventPublisher.publishToChat(ImEventType.CHAT_DELETED, chatId, actorId, chat);
            return;
        }
        removeMember(chatId, actorId);
    }

    /**
     * 退出私聊会话。
     *
     * <p>私聊删除语义是“退出当前用户会话视角”，不会物理删除消息。该操作仅移除当前用户成员关系，
     * 并记录审计日志用于追踪会话退出行为。</p>
     *
     * @param chatId 私聊 ID
     * @param userId 当前用户 ID
     */
    private void leavePrivateChat(Long chatId, Long userId) {
        ImChatMember member = selectMember(chatId, userId);
        if (member == null) {
            return;
        }
        chatMemberMapper.deleteById(member.getId());
        auditService.record(chatId, "chat_member", member.getId(), "LEAVE_PRIVATE_CHAT", userId,
            "退出私聊", member);
        eventPublisher.publishToUsers(ImEventType.CHAT_DELETED, chatId, userId, List.of(userId),
            Map.of("id", chatId, "chatId", chatId));
    }

    /**
     * 归档聊天。
     *
     * <p>P0 当前未引入用户级归档表，因此该操作将聊天全局状态改为 archived。后续如需每用户归档，
     * 可迁移到成员扩展字段。</p>
     *
     * @param chatId 聊天 ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void archiveChat(Long chatId) {
        ImChat chat = permissionService.requireReadable(chatId);
        chat.setStatus(ImChatStatus.ARCHIVED.getCode());
        chatMapper.updateById(chat);
        eventPublisher.publishToChat(ImEventType.CHAT_UPDATED, chatId, permissionService.currentUserId(), buildChatSnapshot(chat));
    }

    /**
     * 取消归档聊天。
     *
     * <p>将聊天状态恢复为 active，使其重新出现在聊天列表中。</p>
     *
     * @param chatId 聊天 ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unarchiveChat(Long chatId) {
        ImChat chat = permissionService.requireChat(chatId);
        permissionService.requireMember(chatId, permissionService.currentUserId());
        chat.setStatus(ImChatStatus.ACTIVE.getCode());
        chatMapper.updateById(chat);
        eventPublisher.publishToChat(ImEventType.CHAT_UPDATED, chatId, permissionService.currentUserId(), buildChatSnapshot(chat));
    }

    /**
     * 查询聊天成员列表。
     *
     * <p>只有聊天成员能查看成员列表。</p>
     *
     * @param chatId 聊天 ID
     * @return 成员列表
     */
    @Override
    public List<ImChatMemberVo> listMembers(Long chatId) {
        permissionService.requireReadable(chatId);
        return chatMemberMapper.selectMembers(chatId);
    }

    /**
     * 添加聊天成员。
     *
     * <p>方法会恢复已存在成员关系或创建新成员，并根据聊天类型标准化角色。</p>
     *
     * @param chatId 聊天 ID
     * @param bo 成员入参
     * @return 成员视图
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ImChatMemberVo addMember(Long chatId, ImChatMemberBo bo) {
        ImChat chat = permissionService.requireChat(chatId);
        if (ImChatType.PRIVATE.getCode().equals(chat.getType()) || ImChatType.SAVED.getCode().equals(chat.getType())) {
            throw new ServiceException("该聊天类型不支持成员管理");
        }
        permissionService.requireManager(chatId);
        requireExistingUser(bo.getUserId());
        ImChatMember member = upsertMember(chatId, bo.getUserId(), normalizeRole(chat.getType(), bo.getRole()));
        Long actorId = permissionService.currentUserId();
        auditService.record(chatId, "chat_member", member.getId(), "ADD_MEMBER", actorId,
            "添加聊天成员", bo);
        eventPublisher.publishToChat(ImEventType.CHAT_UPDATED, chatId, actorId, buildChatSnapshot(chat));
        return findMemberVo(chatId, bo.getUserId());
    }

    /**
     * 移除聊天成员。
     *
     * <p>owner 不能直接移除自己；普通用户只能移除自己；管理员可以移除其他非 owner 成员。</p>
     *
     * @param chatId 聊天 ID
     * @param userId 被移除用户 ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeMember(Long chatId, Long userId) {
        ImChat chat = permissionService.requireChat(chatId);
        if (ImChatType.PRIVATE.getCode().equals(chat.getType()) || ImChatType.SAVED.getCode().equals(chat.getType())) {
            throw new ServiceException("该聊天类型不支持成员管理");
        }
        Long actorId = permissionService.currentUserId();
        String actorRole = permissionService.roleOf(chatId, actorId);
        String targetRole = permissionService.roleOf(chatId, userId);
        if (targetRole == null) {
            return;
        }
        if (!actorId.equals(userId) && !ImMemberRole.isManager(actorRole)) {
            throw new ServiceException("当前用户没有移除成员权限");
        }
        if (ImMemberRole.OWNER.getCode().equals(targetRole)) {
            throw new ServiceException("所有者不能直接移除");
        }
        ImChatMember member = selectMember(chatId, userId);
        chatMemberMapper.deleteById(member.getId());
        auditService.record(chatId, "chat_member", member.getId(), "REMOVE_MEMBER", actorId,
            "移除聊天成员", member);
        eventPublisher.publishToChat(ImEventType.CHAT_UPDATED, chatId, actorId, buildChatSnapshot(chat));
        eventPublisher.publishToUsers(ImEventType.CHAT_DELETED, chatId, actorId, List.of(userId),
            Map.of("id", chatId, "chatId", chatId));
    }

    /**
     * 修改成员角色。
     *
     * <p>管理员可调整成员角色，但不能通过该接口修改私聊或保存的消息成员。</p>
     *
     * @param chatId 聊天 ID
     * @param userId 用户 ID
     * @param bo 成员角色入参
     * @return 修改后的成员视图
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ImChatMemberVo updateMember(Long chatId, Long userId, ImChatMemberBo bo) {
        ImChat chat = permissionService.requireChat(chatId);
        if (ImChatType.PRIVATE.getCode().equals(chat.getType()) || ImChatType.SAVED.getCode().equals(chat.getType())) {
            throw new ServiceException("该聊天类型不支持成员管理");
        }
        permissionService.requireManager(chatId);
        ImChatMember member = selectMember(chatId, userId);
        member.setRole(normalizeRole(chat.getType(), bo.getRole()));
        member.setMuted(StringUtils.blankToDefault(bo.getMuted(), member.getMuted()));
        chatMemberMapper.updateById(member);
        Long actorId = permissionService.currentUserId();
        auditService.record(chatId, "chat_member", member.getId(), "UPDATE_MEMBER", actorId,
            "修改成员角色", bo);
        eventPublisher.publishToChat(ImEventType.CHAT_UPDATED, chatId, actorId, buildChatSnapshot(chat));
        return findMemberVo(chatId, userId);
    }

    /**
     * 构建聊天快照事件载荷。
     *
     * <p>成员变更事件统一发送聊天快照，避免客户端需要同时处理聊天对象和成员对象两种
     * chat.updated 载荷。</p>
     *
     * @param chat 聊天实体
     * @return 聊天视图快照
     */
    private ImChatVo buildChatSnapshot(ImChat chat) {
        ImChatVo vo = new ImChatVo();
        vo.setId(chat.getId());
        vo.setType(chat.getType());
        vo.setTitle(chat.getTitle());
        vo.setAvatar(chat.getAvatar());
        vo.setDescription(chat.getDescription());
        vo.setOwnerId(chat.getOwnerId());
        vo.setSeq(chat.getSeq());
        vo.setLastMsgId(chat.getLastMsgId());
        vo.setLastMsgContent(chat.getLastMsgContent());
        vo.setLastMsgTime(chat.getLastMsgTime());
        vo.setStatus(chat.getStatus());
        vo.setMembers(chatMemberMapper.selectMembers(chat.getId()));
        return vo;
    }

    /**
     * 获取或创建保存的消息聊天。
     *
     * <p>如果用户已有 saved 聊天则直接返回；否则创建一人聊天并设置用户为 owner。</p>
     *
     * @param userId 用户 ID
     * @return 保存的消息聊天实体
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ImChat getOrCreateSavedChat(Long userId) {
        ImChat existing = chatMapper.selectOne(new LambdaQueryWrapper<ImChat>()
            .eq(ImChat::getType, ImChatType.SAVED.getCode())
            .eq(ImChat::getOwnerId, userId)
            .last("LIMIT 1"));
        if (existing != null) {
            return existing;
        }
        ImChat chat = new ImChat();
        chat.setType(ImChatType.SAVED.getCode());
        chat.setTitle("保存的消息");
        chat.setOwnerId(userId);
        chat.setSeq(0L);
        chat.setStatus(ImChatStatus.ACTIVE.getCode());
        chat.setDelFlag(ImConstants.DEL_FLAG_NORMAL);
        chatMapper.insert(chat);
        upsertMember(chat.getId(), userId, ImMemberRole.OWNER.getCode());
        return chat;
    }

    /**
     * 获取或创建两个用户之间的私聊。
     *
     * <p>方法强制两个用户不同且都存在，已存在会话直接复用。</p>
     *
     * @param userId1 用户一 ID
     * @param userId2 用户二 ID
     * @return 私聊实体
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ImChat getOrCreatePrivateChat(Long userId1, Long userId2) {
        if (userId1.equals(userId2)) {
            throw new ServiceException("不能与自己创建私聊");
        }
        requireExistingUser(userId1);
        requireExistingUser(userId2);
        ImChat existing = chatMapper.selectPrivateChat(userId1, userId2);
        if (existing != null) {
            return existing;
        }
        ImChat chat = new ImChat();
        chat.setType(ImChatType.PRIVATE.getCode());
        chat.setSeq(0L);
        chat.setStatus(ImChatStatus.ACTIVE.getCode());
        chat.setDelFlag(ImConstants.DEL_FLAG_NORMAL);
        chatMapper.insert(chat);
        upsertMember(chat.getId(), userId1, ImMemberRole.MEMBER.getCode());
        upsertMember(chat.getId(), userId2, ImMemberRole.MEMBER.getCode());
        auditService.record(chat.getId(), "chat", chat.getId(), "CREATE_PRIVATE_CHAT", userId1,
            "创建私聊", List.of(userId1, userId2));
        return chat;
    }

    /**
     * 创建群组或频道。
     *
     * <p>创建者自动成为 owner，群组会把 memberIds 中的用户加入为 member，频道只创建 owner，
     * 后续通过成员接口添加 subscriber。</p>
     *
     * @param type 聊天类型
     * @param bo 创建入参
     * @param ownerId 创建者 ID
     * @return 新聊天实体
     */
    private ImChat createManagedChat(ImChatType type, ImChatCreateBo bo, Long ownerId) {
        if (StringUtils.isBlank(bo.getTitle())) {
            throw new ServiceException("群组或频道标题不能为空");
        }
        ImChat chat = new ImChat();
        chat.setType(type.getCode());
        chat.setTitle(bo.getTitle());
        chat.setAvatar(bo.getAvatar());
        chat.setDescription(bo.getDescription());
        chat.setOwnerId(ownerId);
        chat.setSeq(0L);
        chat.setStatus(ImChatStatus.ACTIVE.getCode());
        chat.setDelFlag(ImConstants.DEL_FLAG_NORMAL);
        chatMapper.insert(chat);
        upsertMember(chat.getId(), ownerId, ImMemberRole.OWNER.getCode());
        if (type == ImChatType.GROUP) {
            for (Long memberId : distinctMemberIds(bo.getMemberIds(), ownerId)) {
                requireExistingUser(memberId);
                upsertMember(chat.getId(), memberId, ImMemberRole.MEMBER.getCode());
            }
        }
        auditService.record(chat.getId(), "chat", chat.getId(), "CREATE_CHAT", ownerId,
            "创建聊天 " + bo.getTitle(), bo);
        return chat;
    }

    /**
     * 从私聊入参中解析唯一对方用户。
     *
     * <p>私聊必须且只能包含当前用户之外的一个成员，违反该约束会直接拒绝创建。</p>
     *
     * @param memberIds 入参成员列表
     * @param currentUserId 当前用户 ID
     * @return 对方用户 ID
     */
    private Long requireSinglePeer(List<Long> memberIds, Long currentUserId) {
        if (memberIds == null || memberIds.size() != 1) {
            throw new ServiceException("私聊必须指定且只能指定一个对方用户");
        }
        Long peerId = memberIds.get(0);
        if (currentUserId.equals(peerId)) {
            throw new ServiceException("不能与自己创建私聊");
        }
        requireExistingUser(peerId);
        return peerId;
    }

    /**
     * 新增或恢复聊天成员。
     *
     * <p>若成员关系已经存在，则更新角色、加入时间和删除标记；否则创建新成员记录。</p>
     *
     * @param chatId 聊天 ID
     * @param userId 用户 ID
     * @param role 角色编码
     * @return 成员实体
     */
    private ImChatMember upsertMember(Long chatId, Long userId, String role) {
        ImChatMember member = selectMember(chatId, userId);
        if (member == null) {
            member = new ImChatMember();
            member.setChatId(chatId);
            member.setUserId(userId);
            member.setMuted("0");
            member.setJoinedTime(new Date());
            member.setDelFlag(ImConstants.DEL_FLAG_NORMAL);
            member.setRole(role);
            chatMemberMapper.insert(member);
            return member;
        }
        member.setRole(role);
        member.setDelFlag(ImConstants.DEL_FLAG_NORMAL);
        chatMemberMapper.updateById(member);
        return member;
    }

    /**
     * 查询成员实体。
     *
     * <p>该方法不抛异常，便于 upsert 流程判断成员是否已存在。</p>
     *
     * @param chatId 聊天 ID
     * @param userId 用户 ID
     * @return 成员实体或 null
     */
    private ImChatMember selectMember(Long chatId, Long userId) {
        return chatMemberMapper.selectOne(new LambdaQueryWrapper<ImChatMember>()
            .eq(ImChatMember::getChatId, chatId)
            .eq(ImChatMember::getUserId, userId)
            .last("LIMIT 1"));
    }

    /**
     * 查询成员视图并要求存在。
     *
     * <p>该方法从成员列表视图中定位指定用户，确保返回对象包含用户名、昵称和头像。</p>
     *
     * @param chatId 聊天 ID
     * @param userId 用户 ID
     * @return 成员视图
     */
    private ImChatMemberVo findMemberVo(Long chatId, Long userId) {
        return chatMemberMapper.selectMembers(chatId).stream()
            .filter(item -> userId.equals(item.getUserId()))
            .findFirst()
            .orElseThrow(() -> new ServiceException("聊天成员不存在"));
    }

    /**
     * 按聊天类型标准化角色。
     *
     * <p>频道默认 subscriber，群组默认 member。传入 admin 或 owner 时仍会校验枚举合法性。</p>
     *
     * @param chatType 聊天类型编码
     * @param role 入参角色
     * @return 标准化角色编码
     */
    private String normalizeRole(String chatType, String role) {
        if (StringUtils.isBlank(role)) {
            return ImChatType.CHANNEL.getCode().equals(chatType)
                ? ImMemberRole.SUBSCRIBER.getCode()
                : ImMemberRole.MEMBER.getCode();
        }
        ImMemberRole targetRole = ImMemberRole.require(role);
        if (targetRole == ImMemberRole.OWNER) {
            throw new ServiceException("所有者角色只能通过所有权转让调整");
        }
        if (ImChatType.CHANNEL.getCode().equals(chatType)) {
            if (targetRole != ImMemberRole.ADMIN && targetRole != ImMemberRole.SUBSCRIBER) {
                throw new ServiceException("频道成员角色只能是 admin 或 subscriber");
            }
            return targetRole.getCode();
        }
        if (targetRole != ImMemberRole.ADMIN && targetRole != ImMemberRole.MEMBER) {
            throw new ServiceException("群组成员角色只能是 admin 或 member");
        }
        return targetRole.getCode();
    }

    /**
     * 去重并排除创建者 ID。
     *
     * <p>群组创建时 memberIds 可能包含重复值或创建者本人，该方法确保只为额外成员创建关系。</p>
     *
     * @param memberIds 原始成员 ID 列表
     * @param ownerId 创建者 ID
     * @return 去重后的成员 ID 列表
     */
    private List<Long> distinctMemberIds(List<Long> memberIds, Long ownerId) {
        if (memberIds == null || memberIds.isEmpty()) {
            return List.of();
        }
        Set<Long> set = new LinkedHashSet<>(memberIds);
        set.remove(ownerId);
        return new ArrayList<>(set);
    }

    /**
     * 校验用户存在。
     *
     * <p>成员管理和私聊创建都必须确保目标用户是有效 IM 用户。</p>
     *
     * @param userId 用户 ID
     */
    private void requireExistingUser(Long userId) {
        ImUser user = userMapper.selectById(userId);
        if (user == null || !"0".equals(user.getStatus())) {
            throw new ServiceException("用户不存在或已停用");
        }
    }
}
