package com.AgentIM.business.im.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.AgentIM.business.im.domain.bo.ImMessageEditBo;
import com.AgentIM.business.im.domain.bo.ImMessageSendBo;
import com.AgentIM.business.im.domain.bo.ImReactionBo;
import com.AgentIM.business.im.domain.vo.ImMessageReadStatusVo;
import com.AgentIM.business.im.domain.vo.ImMessageVo;
import com.AgentIM.business.im.domain.vo.ImPinnedMessageVo;
import com.AgentIM.business.im.domain.vo.ImReactionVo;
import com.AgentIM.business.im.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * IM 消息接口。
 *
 * <p>该 Controller 聚合消息列表、发送、编辑、删除、反应、已读和置顶操作。所有接口只处理后端
 * 数据契约，不包含前端实现。</p>
 */
@Validated
@RestController
@RequiredArgsConstructor
public class ImMessageController {

    private final IImMessageService messageService;
    private final IImReactionService reactionService;
    private final IImReadStateService readStateService;
    private final IImPinnedMessageService pinnedMessageService;

    /**
     * 查询聊天消息列表。
     *
     * <p>使用 beforeSeq 游标向历史翻页，返回当前用户可见且未隐藏的消息。</p>
     *
     * @param chatId 聊天 ID
     * @param beforeSeq 历史游标
     * @param limit 最大返回条数
     * @return 消息列表响应
     */
    @SaCheckPermission("im:message:read")
    @GetMapping("/im/chats/{chatId}/messages")
    public R<List<ImMessageVo>> listMessages(@PathVariable Long chatId,
                                             @RequestParam(value = "beforeSeq", required = false) Long beforeSeq,
                                             @RequestParam(value = "limit", defaultValue = "30") int limit) {
        return R.ok(messageService.listMessages(chatId, beforeSeq, limit));
    }

    /**
     * 发送消息。
     *
     * <p>支持文本、媒体、投票、引用回复和转发消息。媒体消息必须引用已经上传的资源 ID。</p>
     *
     * @param chatId 聊天 ID
     * @param bo 发送消息入参
     * @return 新消息视图响应
     */
    @SaCheckPermission("im:message:send")
    @PostMapping("/im/chats/{chatId}/messages")
    public R<ImMessageVo> sendMessage(@PathVariable Long chatId, @Valid @RequestBody ImMessageSendBo bo) {
        return R.ok(messageService.sendMessage(chatId, bo));
    }

    /**
     * 编辑消息。
     *
     * <p>只有发送者可在编辑窗口期内修改消息内容、结构化载荷和资源引用。</p>
     *
     * @param messageId 消息 ID
     * @param bo 编辑入参
     * @return 编辑后的消息视图响应
     */
    @SaCheckPermission("im:message:edit")
    @PutMapping("/im/messages/{messageId}")
    public R<ImMessageVo> editMessage(@PathVariable Long messageId, @Valid @RequestBody ImMessageEditBo bo) {
        return R.ok(messageService.editMessage(messageId, bo));
    }

    /**
     * 对所有人删除消息。
     *
     * <p>发送者或聊天管理员可在撤回窗口期内执行，消息状态会变为 deleted_all。</p>
     *
     * @param messageId 消息 ID
     * @return 操作结果响应
     */
    @SaCheckPermission("im:message:delete")
    @DeleteMapping("/im/messages/{messageId}")
    public R<Void> deleteForAll(@PathVariable Long messageId) {
        messageService.deleteForAll(messageId);
        return R.ok();
    }

    /**
     * 仅自己隐藏消息。
     *
     * <p>当前用户隐藏后，消息只在自己的列表中不可见，其他成员仍可正常查看。</p>
     *
     * @param messageId 消息 ID
     * @return 操作结果响应
     */
    @SaCheckPermission("im:message:delete")
    @PostMapping("/im/messages/{messageId}/hide")
    public R<Void> hideForSelf(@PathVariable Long messageId) {
        messageService.hideForSelf(messageId);
        return R.ok();
    }

    /**
     * 查询消息反应明细。
     *
     * <p>返回每条 reaction 对应的用户信息，调用者必须能读取该消息所属聊天。</p>
     *
     * @param messageId 消息 ID
     * @return 反应明细列表响应
     */
    @SaCheckPermission("im:message:read")
    @GetMapping("/im/messages/{messageId}/reactions")
    public R<List<ImReactionVo>> listReactions(@PathVariable Long messageId) {
        return R.ok(reactionService.listReactions(messageId));
    }

    /**
     * 添加消息反应。
     *
     * <p>同一用户对同一消息添加同一反应保持幂等。</p>
     *
     * @param messageId 消息 ID
     * @param bo 反应入参
     * @return 反应视图响应
     */
    @SaCheckPermission("im:reaction:add")
    @PostMapping("/im/messages/{messageId}/reactions")
    public R<ImReactionVo> addReaction(@PathVariable Long messageId, @Valid @RequestBody ImReactionBo bo) {
        return R.ok(reactionService.addReaction(messageId, bo));
    }

    /**
     * 删除消息反应。
     *
     * <p>普通用户只能删除自己的反应，管理员可以删除任意成员的反应。</p>
     *
     * @param messageId 消息 ID
     * @param userId 反应所属用户 ID
     * @param reaction 反应标识
     * @return 操作结果响应
     */
    @SaCheckPermission("im:reaction:add")
    @DeleteMapping("/im/messages/{messageId}/reactions/{userId}/{reaction}")
    public R<Void> deleteReaction(@PathVariable Long messageId,
                                  @PathVariable Long userId,
                                  @PathVariable String reaction) {
        reactionService.deleteReaction(messageId, userId, reaction);
        return R.ok();
    }

    /**
     * 标记消息已读。
     *
     * <p>服务端会把当前用户在该聊天的已读游标推进到目标消息，游标只前进不后退。</p>
     *
     * @param messageId 消息 ID
     * @return 操作结果响应
     */
    @SaCheckPermission("im:message:read")
    @PostMapping("/im/messages/{messageId}/read")
    public R<Void> markRead(@PathVariable Long messageId) {
        readStateService.markRead(messageId);
        return R.ok();
    }

    /**
     * 查询聊天未读数。
     *
     * <p>未读数按当前用户已读游标、消息状态和隐藏记录计算。</p>
     *
     * @param chatId 聊天 ID
     * @return 未读数响应
     */
    @SaCheckPermission("im:message:read")
    @GetMapping("/im/chats/{chatId}/unread-count")
    public R<Long> unreadCount(@PathVariable Long chatId) {
        return R.ok(readStateService.unreadCount(chatId));
    }

    /**
     * 查询消息已读状态。
     *
     * <p>返回 readBy、unreadBy、已读人数和比例，支持群聊已读人头和饼图展示。</p>
     *
     * @param messageId 消息 ID
     * @return 已读状态响应
     */
    @SaCheckPermission("im:message:read")
    @GetMapping("/im/messages/{messageId}/read-status")
    public R<ImMessageReadStatusVo> readStatus(@PathVariable Long messageId) {
        return R.ok(readStateService.getReadStatus(messageId));
    }

    /**
     * 置顶消息。
     *
     * <p>私聊双方可置顶，群组和频道要求 owner/admin 权限。</p>
     *
     * @param messageId 消息 ID
     * @return 置顶记录响应
     */
    @SaCheckPermission("im:message:pin")
    @PostMapping("/im/messages/{messageId}/pin")
    public R<ImPinnedMessageVo> pinMessage(@PathVariable Long messageId) {
        return R.ok(pinnedMessageService.pinMessage(messageId));
    }

    /**
     * 取消置顶消息。
     *
     * <p>取消置顶只删除置顶关系，不删除消息本身。</p>
     *
     * @param messageId 消息 ID
     * @return 操作结果响应
     */
    @SaCheckPermission("im:message:pin")
    @DeleteMapping("/im/messages/{messageId}/pin")
    public R<Void> unpinMessage(@PathVariable Long messageId) {
        pinnedMessageService.unpinMessage(messageId);
        return R.ok();
    }
}
