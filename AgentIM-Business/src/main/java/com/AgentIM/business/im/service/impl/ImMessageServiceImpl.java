package com.AgentIM.business.im.service.impl;

import cn.hutool.core.lang.Dict;
import com.AgentIM.business.im.constant.ImEventType;
import com.AgentIM.business.im.domain.bo.ImMessageEditBo;
import com.AgentIM.business.im.domain.bo.ImMessageSendBo;
import com.AgentIM.business.im.domain.entity.*;
import com.AgentIM.business.im.domain.vo.ImMessageVo;
import com.AgentIM.business.im.enums.ImMessageStatus;
import com.AgentIM.business.im.enums.ImMessageType;
import com.AgentIM.business.im.enums.ImPollStatus;
import com.AgentIM.business.im.mapper.*;
import com.AgentIM.business.im.permission.ImPermissionService;
import com.AgentIM.business.im.service.IImMessageService;
import com.AgentIM.business.im.service.IImReadStateService;
import com.AgentIM.business.im.support.ImAuditService;
import com.AgentIM.business.im.support.ImEventPublisher;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.json.utils.JsonUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * 消息服务实现。
 */
@Service
@RequiredArgsConstructor
public class ImMessageServiceImpl implements IImMessageService {

    private final ImMessageMapper messageMapper;
    private final ImMessageReplyMapper messageReplyMapper;
    private final ImMessageReactionMapper reactionMapper;
    private final ImChatMapper chatMapper;
    private final ImResourceMapper resourceMapper;
    private final ImUserMessageHideMapper hideMapper;
    private final ImPollMapper pollMapper;
    private final ImPollOptionMapper pollOptionMapper;
    private final ImPermissionService permissionService;
    private final IImReadStateService readStateService;
    private final ImAuditService auditService;
    private final ImEventPublisher eventPublisher;

    @Value("${agentim.im.message-edit-window-hours:48}")
    private long editWindowHours;

    @Value("${agentim.im.message-delete-window-hours:48}")
    private long deleteWindowHours;

    /**
     * 查询聊天消息列表。
     *
     * <p>当前用户必须是聊天成员。limit 会被限制在 1 到 100，避免单次游标分页拉取过多消息。</p>
     *
     * @param chatId 聊天 ID
     * @param beforeSeq 历史游标
     * @param limit 最大返回条数
     * @return 消息列表
     */
    @Override
    public List<ImMessageVo> listMessages(Long chatId, Long beforeSeq, int limit) {
        permissionService.requireReadable(chatId);
        List<ImMessageVo> messages = messageMapper.selectVisibleMessages(
            chatId,
            permissionService.currentUserId(),
            beforeSeq,
            normalizeLimit(limit)
        );
        messages.forEach(this::fillReactions);
        return messages;
    }

    /**
     * 发送消息。
     *
     * <p>方法严格按文档流程执行：权限校验、资源校验、幂等检查、消息构建、关联表写入、聊天摘要
     * 更新、发送者已读和事务后事件发布。事务内只做数据库操作。</p>
     *
     * @param chatId 聊天 ID
     * @param bo 发送消息入参
     * @return 新消息或幂等命中消息视图
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ImMessageVo sendMessage(Long chatId, ImMessageSendBo bo) {
        if (StringUtils.isBlank(bo.getIdempotentKey())) {
            throw new ServiceException("幂等键不能为空");
        }
        permissionService.requireWritable(chatId);
        ImMessageType type = ImMessageType.require(bo.getMessageType());
        validateMediaResources(type, bo.getResourceIds());
        ImMessage existing = messageMapper.selectByIdempotentKey(chatId, bo.getIdempotentKey());
        if (existing != null) {
            return getMessageVo(existing.getId());
        }

        ImMessage replyTo = resolveReplyMessage(chatId, bo.getReplyToMessageId());
        ImMessage forwardFrom = resolveForwardMessage(bo.getForwardFromMessageId());
        Long nextSeq = chatMapper.nextSeq(chatId);
        if (nextSeq == null) {
            throw new ServiceException("聊天序号分配失败");
        }

        ImMessage message = buildMessage(chatId, bo, type, forwardFrom, nextSeq);
        try {
            messageMapper.insert(message);
        } catch (DuplicateKeyException ex) {
            ImMessage duplicate = messageMapper.selectByIdempotentKey(chatId, bo.getIdempotentKey());
            if (duplicate != null) {
                return getMessageVo(duplicate.getId());
            }
            throw ex;
        }
        if (replyTo != null) {
            createReplyIndex(message, replyTo);
        }
        if (type == ImMessageType.POLL) {
            createPoll(message, bo);
        }
        bindResources(chatId, message.getId(), bo.getResourceIds());
        chatMapper.updateLastMessage(chatId, nextSeq, message.getId(), preview(message.getContent()));
        readStateService.markRead(message.getSenderId(), message);

        ImMessageVo vo = getMessageVo(message.getId());
        eventPublisher.publishToChat(ImEventType.MESSAGE_CREATED, chatId, message.getSenderId(), vo);
        return vo;
    }

    /**
     * 编辑消息。
     *
     * <p>编辑要求当前用户是发送者，且消息仍在配置窗口期内。编辑成功后状态变为 edited。</p>
     *
     * @param messageId 消息 ID
     * @param bo 编辑入参
     * @return 编辑后的消息视图
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ImMessageVo editMessage(Long messageId, ImMessageEditBo bo) {
        ImMessage message = requireMessage(messageId);
        permissionService.requireReadable(message.getChatId());
        Long actorId = permissionService.currentUserId();
        if (!actorId.equals(message.getSenderId())) {
            throw new ServiceException("只能编辑自己发送的消息");
        }
        requireWithinWindow(message.getCreateTime(), editWindowHours, "消息已超过可编辑时间");
        String oldContent = message.getContent();
        message.setContent(bo.getContent());
        message.setContentPayload(bo.getContentPayload());
        validateBindableResourcesForMessage(message.getChatId(), message.getId(), bo.getResourceIds());
        message.setResourceIds(JsonUtils.toJsonString(bo.getResourceIds()));
        message.setStatus(ImMessageStatus.EDITED.getCode());
        message.setEditTime(new Date());
        messageMapper.updateById(message);
        bindResources(message.getChatId(), message.getId(), bo.getResourceIds());
        ImMessageVo vo = getMessageVo(messageId);
        auditService.record(message.getChatId(), "message", messageId, "EDIT_MESSAGE", actorId,
            "编辑消息", Dict.create().set("old", oldContent).set("new", bo.getContent()));
        eventPublisher.publishToChat(ImEventType.MESSAGE_EDITED, message.getChatId(), actorId, vo);
        return vo;
    }

    /**
     * 对所有人删除消息。
     *
     * <p>发送者或管理员可以在配置窗口期内撤回消息。该方法只修改状态和删除时间，保留消息记录供审计。</p>
     *
     * @param messageId 消息 ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteForAll(Long messageId) {
        ImMessage message = requireMessage(messageId);
        permissionService.requireReadable(message.getChatId());
        if (!permissionService.canDeleteMessage(message)) {
            throw new ServiceException("当前用户无权删除该消息");
        }
        requireWithinWindow(message.getCreateTime(), deleteWindowHours, "消息已超过可撤回时间");
        Long actorId = permissionService.currentUserId();
        String oldContent = message.getContent();
        message.setStatus(ImMessageStatus.DELETED_ALL.getCode());
        message.setDeleteTime(new Date());
        messageMapper.updateById(message);
        auditService.record(message.getChatId(), "message", messageId, "DELETE_MESSAGE_ALL", actorId,
            "对所有人删除消息", Dict.create().set("content", oldContent));
        eventPublisher.publishToChat(ImEventType.MESSAGE_DELETED, message.getChatId(), actorId,
            Dict.create().set("messageId", messageId).set("chatId", message.getChatId()));
    }

    /**
     * 仅当前用户隐藏消息。
     *
     * <p>隐藏记录保持幂等，重复隐藏不会产生额外数据。事件只发给操作者，用于多设备同步。</p>
     *
     * @param messageId 消息 ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void hideForSelf(Long messageId) {
        ImMessage message = requireMessage(messageId);
        permissionService.requireReadable(message.getChatId());
        Long userId = permissionService.currentUserId();
        ImUserMessageHide existing = hideMapper.selectOne(new LambdaQueryWrapper<ImUserMessageHide>()
            .eq(ImUserMessageHide::getUserId, userId)
            .eq(ImUserMessageHide::getMessageId, messageId)
            .last("LIMIT 1"));
        if (existing == null) {
            ImUserMessageHide hide = new ImUserMessageHide();
            hide.setUserId(userId);
            hide.setMessageId(messageId);
            hide.setHiddenTime(new Date());
            hide.setDelFlag("0");
            hideMapper.insert(hide);
        }
        eventPublisher.publishToUsers(ImEventType.MESSAGE_HIDDEN, message.getChatId(), userId, List.of(userId),
            Dict.create().set("messageId", messageId).set("chatId", message.getChatId()).set("userId", userId));
    }

    /**
     * 根据 ID 获取消息并要求存在。
     *
     * <p>委托权限服务做统一的不存在判断。</p>
     *
     * @param messageId 消息 ID
     * @return 消息实体
     */
    @Override
    public ImMessage requireMessage(Long messageId) {
        return permissionService.requireMessage(messageId);
    }

    /**
     * 根据 ID 查询消息视图。
     *
     * <p>返回对象会补充反应聚合信息，便于客户端无需额外请求即可展示基础 reaction 摘要。</p>
     *
     * @param messageId 消息 ID
     * @return 消息视图
     */
    @Override
    public ImMessageVo getMessageVo(Long messageId) {
        ImMessage message = requireMessage(messageId);
        ImMessageVo vo = MapstructUtils.convert(message, ImMessageVo.class);
        fillReactions(vo);
        return vo;
    }

    /**
     * 构建消息实体。
     *
     * <p>该方法只组装内存对象，不访问数据库。发送者、类型、资源 JSON、转发来源和 seq 都在这里
     * 标准化。</p>
     *
     * @param chatId 聊天 ID
     * @param bo 发送入参
     * @param type 消息类型
     * @param forwardFrom 转发来源消息，可为空
     * @param nextSeq 新消息序号
     * @return 待插入消息实体
     */
    private ImMessage buildMessage(Long chatId, ImMessageSendBo bo, ImMessageType type, ImMessage forwardFrom, Long nextSeq) {
        ImMessage message = new ImMessage();
        message.setChatId(chatId);
        message.setSenderId(permissionService.currentUserId());
        message.setMessageType(type.getCode());
        message.setContent(bo.getContent());
        message.setContentPayload(bo.getContentPayload());
        message.setResourceIds(JsonUtils.toJsonString(bo.getResourceIds()));
        message.setReplyToMessageId(bo.getReplyToMessageId());
        message.setForwardFromMessageId(bo.getForwardFromMessageId());
        message.setForwardFromChatId(resolveForwardFromChatId(forwardFrom));
        message.setForwardSenderId(forwardFrom == null ? null : forwardFrom.getSenderId());
        message.setClientMsgId(bo.getClientMsgId());
        message.setIdempotentKey(bo.getIdempotentKey());
        message.setSeq(nextSeq);
        message.setReplyCount(0);
        message.setStatus(ImMessageStatus.NORMAL.getCode());
        message.setDelFlag("0");
        return message;
    }

    /**
     * 校验媒体消息资源引用。
     *
     * <p>图片、文件、语音和视频消息必须携带至少一个资源 ID，避免消息表直接保存文件内容或出现
     * 无资源媒体消息。</p>
     *
     * @param type 消息类型
     * @param resourceIds 资源 ID 列表
     */
    private void validateMediaResources(ImMessageType type, List<Long> resourceIds) {
        if (ImMessageType.isMedia(type.getCode()) && (resourceIds == null || resourceIds.isEmpty())) {
            throw new ServiceException("媒体消息必须引用资源ID");
        }
    }

    /**
     * 校验资源是否允许绑定到当前消息。
     *
     * <p>资源必须由当前用户上传，并且只能是尚未绑定的资源或当前消息已绑定的资源。这样既允许编辑
     * 消息保留原附件，也避免把其他用户或其他消息的附件挪用到当前消息。</p>
     *
     * @param chatId 聊天 ID
     * @param messageId 消息 ID
     * @param resourceIds 资源 ID 列表
     */
    void validateBindableResourcesForMessage(Long chatId, Long messageId, List<Long> resourceIds) {
        List<Long> normalizedIds = normalizeResourceIds(resourceIds);
        if (normalizedIds.isEmpty()) {
            return;
        }
        Long userId = permissionService.currentUserId();
        int bindableCount = resourceMapper.countBindableResources(chatId, messageId, userId, normalizedIds);
        if (bindableCount != normalizedIds.size()) {
            throw new ServiceException("存在无权绑定或不存在的资源");
        }
    }

    /**
     * 解析转发来源聊天。
     *
     * <p>来源聊天以服务端加载到的来源消息为准，忽略客户端传入的 forwardFromChatId，防止客户端伪造
     * 转发来源。</p>
     *
     * @param forwardFrom 转发来源消息
     * @return 转发来源聊天 ID
     */
    Long resolveForwardFromChatId(ImMessage forwardFrom) {
        return forwardFrom == null ? null : forwardFrom.getChatId();
    }

    /**
     * 解析并校验引用回复消息。
     *
     * <p>被回复消息必须存在、属于同一个聊天并且当前用户对该聊天可读。</p>
     *
     * @param chatId 当前聊天 ID
     * @param replyToMessageId 被回复消息 ID
     * @return 被回复消息实体，可为空
     */
    private ImMessage resolveReplyMessage(Long chatId, Long replyToMessageId) {
        if (replyToMessageId == null) {
            return null;
        }
        ImMessage replyTo = requireMessage(replyToMessageId);
        if (!chatId.equals(replyTo.getChatId())) {
            throw new ServiceException("只能回复同一聊天内的消息");
        }
        permissionService.requireReadable(replyTo.getChatId());
        return replyTo;
    }

    /**
     * 解析并校验转发来源消息。
     *
     * <p>转发来源消息必须存在且当前用户对来源聊天可读。方法返回来源消息，用于记录原发送者。</p>
     *
     * @param forwardFromMessageId 转发来源消息 ID
     * @return 来源消息实体，可为空
     */
    private ImMessage resolveForwardMessage(Long forwardFromMessageId) {
        if (forwardFromMessageId == null) {
            return null;
        }
        ImMessage forwardFrom = requireMessage(forwardFromMessageId);
        permissionService.requireReadable(forwardFrom.getChatId());
        return forwardFrom;
    }

    /**
     * 创建回复索引。
     *
     * <p>回复索引和原消息 replyCount 与消息主表在同一事务内更新，保证列表展示与回复查询一致。</p>
     *
     * @param message 新回复消息
     * @param replyTo 被回复消息
     */
    private void createReplyIndex(ImMessage message, ImMessage replyTo) {
        ImMessageReply reply = new ImMessageReply();
        reply.setMessageId(message.getId());
        reply.setReplyToMessageId(replyTo.getId());
        reply.setChatId(message.getChatId());
        reply.setDelFlag("0");
        messageReplyMapper.insert(reply);
        messageMapper.incrementReplyCount(replyTo.getId());
    }

    /**
     * 创建投票主体和选项。
     *
     * <p>投票消息的 content 作为问题，contentPayload.options 作为选项数组。没有选项时拒绝发送，
     * 避免创建不可投票的 poll 消息。</p>
     *
     * @param message 投票消息
     * @param bo 发送入参
     */
    private void createPoll(ImMessage message, ImMessageSendBo bo) {
        List<String> options = extractPollOptions(bo.getContentPayload());
        if (options.size() < 2) {
            throw new ServiceException("投票至少需要两个选项");
        }
        Dict payload = JsonUtils.parseMap(bo.getContentPayload());
        ImPoll poll = new ImPoll();
        poll.setMessageId(message.getId());
        poll.setQuestion(StringUtils.blankToDefault(message.getContent(), "投票"));
        poll.setMultiple(Boolean.TRUE.equals(payload == null ? null : payload.getBool("multiple")) ? "1" : "0");
        poll.setAnonymous(Boolean.TRUE.equals(payload == null ? null : payload.getBool("anonymous")) ? "1" : "0");
        poll.setStatus(ImPollStatus.ACTIVE.getCode());
        poll.setDelFlag("0");
        pollMapper.insert(poll);
        for (int i = 0; i < options.size(); i++) {
            ImPollOption option = new ImPollOption();
            option.setPollId(poll.getId());
            option.setText(options.get(i));
            option.setOrdinal(i + 1);
            option.setDelFlag("0");
            pollOptionMapper.insert(option);
        }
    }

    /**
     * 从投票 JSON 载荷中提取选项文本。
     *
     * <p>方法接受 {"options":["A","B"]} 格式，过滤空选项并保持原始顺序。</p>
     *
     * @param contentPayload JSON 字符串载荷
     * @return 选项文本列表
     */
    private List<String> extractPollOptions(String contentPayload) {
        Dict payload = JsonUtils.parseMap(contentPayload);
        if (payload == null) {
            return List.of();
        }
        Object rawOptions = payload.get("options");
        if (!(rawOptions instanceof List<?> rawList)) {
            return List.of();
        }
        List<String> options = new ArrayList<>();
        for (Object item : rawList) {
            if (item != null && StringUtils.isNotBlank(String.valueOf(item))) {
                options.add(String.valueOf(item).trim());
            }
        }
        return options;
    }

    /**
     * 将资源绑定到消息。
     *
     * <p>没有资源时直接返回。绑定动作只更新 IM 资源元信息，不调用外部文件服务，符合事务内不做
     * 外部调用的约束。</p>
     *
     * @param chatId 聊天 ID
     * @param messageId 消息 ID
     * @param resourceIds 资源 ID 列表
     */
    private void bindResources(Long chatId, Long messageId, List<Long> resourceIds) {
        List<Long> normalizedIds = normalizeResourceIds(resourceIds);
        if (normalizedIds.isEmpty()) {
            return;
        }
        validateBindableResourcesForMessage(chatId, messageId, normalizedIds);
        Long userId = permissionService.currentUserId();
        int affectedRows = resourceMapper.bindToMessage(chatId, messageId, userId, normalizedIds);
        if (affectedRows != normalizedIds.size()) {
            throw new ServiceException("资源绑定失败");
        }
    }

    /**
     * 标准化资源 ID 列表。
     *
     * <p>空列表直接返回，重复 ID 只保留一次；出现空 ID 时抛出业务异常，避免无效资源引用进入 SQL。</p>
     *
     * @param resourceIds 原始资源 ID 列表
     * @return 去重后的资源 ID 列表
     */
    private List<Long> normalizeResourceIds(List<Long> resourceIds) {
        if (resourceIds == null || resourceIds.isEmpty()) {
            return List.of();
        }
        if (resourceIds.stream().anyMatch(Objects::isNull)) {
            throw new ServiceException("资源ID不能为空");
        }
        return resourceIds.stream().distinct().toList();
    }

    /**
     * 生成聊天列表消息预览。
     *
     * <p>预览最多保留 100 个字符，避免聊天列表字段过长。</p>
     *
     * @param content 消息正文
     * @return 预览文本
     */
    private String preview(String content) {
        if (StringUtils.isBlank(content)) {
            return "";
        }
        return content.length() > 100 ? content.substring(0, 100) : content;
    }

    /**
     * 校验消息是否仍在操作窗口内。
     *
     * <p>编辑和撤回都有 48 小时默认窗口，配置可通过 agentim.im.* 覆盖。</p>
     *
     * @param createTime 消息创建时间
     * @param hours 窗口小时数
     * @param message 过期提示
     */
    private void requireWithinWindow(Date createTime, long hours, String message) {
        if (createTime == null) {
            return;
        }
        long deadline = createTime.getTime() + hours * 60 * 60 * 1000;
        if (System.currentTimeMillis() > deadline) {
            throw new ServiceException(message);
        }
    }

    /**
     * 填充消息反应聚合。
     *
     * <p>消息列表和详情返回时使用相同聚合逻辑，保持客户端展示一致。</p>
     *
     * @param vo 消息视图
     */
    private void fillReactions(ImMessageVo vo) {
        vo.setReactions(reactionMapper.selectSummaryByMessageId(vo.getId()));
    }

    /**
     * 收敛消息分页条数。
     *
     * <p>默认 30 条，最大 100 条，防止一次请求返回过多历史消息。</p>
     *
     * @param limit 调用方传入条数
     * @return 规范化后的条数
     */
    private int normalizeLimit(int limit) {
        if (limit <= 0) {
            return 30;
        }
        return Math.min(limit, 100);
    }
}
