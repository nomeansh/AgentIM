package com.AgentIM.business.im.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.AgentIM.business.im.domain.bo.ImChatCreateBo;
import com.AgentIM.business.im.domain.bo.ImChatMemberBo;
import com.AgentIM.business.im.domain.bo.ImChatUpdateBo;
import com.AgentIM.business.im.domain.vo.ImChatMemberVo;
import com.AgentIM.business.im.domain.vo.ImChatVo;
import com.AgentIM.business.im.domain.vo.ImPinnedMessageVo;
import com.AgentIM.business.im.permission.ImPermissionService;
import com.AgentIM.business.im.service.IImChatService;
import com.AgentIM.business.im.service.IImPinnedMessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * IM 聊天会话接口。
 *
 * <p>聊天统一承载私聊、群组、频道和保存的消息。成员管理接口只适用于群组和频道。</p>
 */
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/im/chats")
public class ImChatController {

    private final IImChatService chatService;
    private final IImPinnedMessageService pinnedMessageService;
    private final ImPermissionService permissionService;

    /**
     * 查询当前用户聊天列表。
     *
     * <p>返回 active 状态聊天，并附带最后消息预览和未读数。</p>
     *
     * @return 聊天列表响应
     */
    @SaCheckPermission("im:chat:list")
    @GetMapping
    public R<List<ImChatVo>> listChats() {
        return R.ok(chatService.listChats());
    }

    /**
     * 获取保存的消息聊天。
     *
     * <p>如果历史数据缺失保存的消息聊天，服务层会为当前用户补建一个 saved 聊天。</p>
     *
     * @return 保存的消息聊天详情响应
     */
    @SaCheckPermission("im:chat:list")
    @GetMapping("/saved")
    public R<ImChatVo> savedChat() {
        Long userId = permissionService.currentUserId();
        return R.ok(chatService.getChat(chatService.getOrCreateSavedChat(userId).getId()));
    }

    /**
     * 获取聊天详情。
     *
     * <p>详情包含聊天基本信息和成员列表，调用者必须是聊天成员。</p>
     *
     * @param chatId 聊天 ID
     * @return 聊天详情响应
     */
    @SaCheckPermission("im:chat:list")
    @GetMapping("/{chatId}")
    public R<ImChatVo> getChat(@PathVariable Long chatId) {
        return R.ok(chatService.getChat(chatId));
    }

    /**
     * 创建聊天。
     *
     * <p>支持私聊、群组和频道。私聊会复用已有会话，保存的消息不能通过该接口创建。</p>
     *
     * @param bo 创建聊天入参
     * @return 聊天详情响应
     */
    @SaCheckPermission("im:chat:create")
    @PostMapping
    public R<ImChatVo> createChat(@Valid @RequestBody ImChatCreateBo bo) {
        return R.ok(chatService.createChat(bo));
    }

    /**
     * 更新聊天设置。
     *
     * <p>仅群组和频道 owner/admin 可更新标题、头像和简介。</p>
     *
     * @param chatId 聊天 ID
     * @param bo 更新入参
     * @return 更新后的聊天详情响应
     */
    @SaCheckPermission("im:chat:update")
    @PutMapping("/{chatId}")
    public R<ImChatVo> updateChat(@PathVariable Long chatId, @Valid @RequestBody ImChatUpdateBo bo) {
        return R.ok(chatService.updateChat(chatId, bo));
    }

    /**
     * 删除或退出聊天。
     *
     * <p>管理员调用会关闭聊天，普通成员调用会退出聊天。</p>
     *
     * @param chatId 聊天 ID
     * @return 操作结果响应
     */
    @SaCheckPermission("im:chat:delete")
    @DeleteMapping("/{chatId}")
    public R<Void> deleteChat(@PathVariable Long chatId) {
        chatService.deleteChat(chatId);
        return R.ok();
    }

    /**
     * 归档聊天。
     *
     * <p>归档后聊天不再出现在主列表，消息仍保留。</p>
     *
     * @param chatId 聊天 ID
     * @return 操作结果响应
     */
    @SaCheckPermission("im:chat:update")
    @PostMapping("/{chatId}/archive")
    public R<Void> archiveChat(@PathVariable Long chatId) {
        chatService.archiveChat(chatId);
        return R.ok();
    }

    /**
     * 取消归档聊天。
     *
     * <p>取消归档会将聊天恢复到 active 状态。</p>
     *
     * @param chatId 聊天 ID
     * @return 操作结果响应
     */
    @SaCheckPermission("im:chat:update")
    @PostMapping("/{chatId}/unarchive")
    public R<Void> unarchiveChat(@PathVariable Long chatId) {
        chatService.unarchiveChat(chatId);
        return R.ok();
    }

    /**
     * 查询聊天成员列表。
     *
     * <p>只有聊天成员可以查看该列表。</p>
     *
     * @param chatId 聊天 ID
     * @return 成员列表响应
     */
    @SaCheckPermission("im:chat:list")
    @GetMapping("/{chatId}/members")
    public R<List<ImChatMemberVo>> listMembers(@PathVariable Long chatId) {
        return R.ok(chatService.listMembers(chatId));
    }

    /**
     * 添加聊天成员。
     *
     * <p>适用于群组和频道。私聊成员固定为两人，不能通过该接口变更。</p>
     *
     * @param chatId 聊天 ID
     * @param bo 成员入参
     * @return 成员视图响应
     */
    @SaCheckPermission("im:chat:member")
    @PostMapping("/{chatId}/members")
    public R<ImChatMemberVo> addMember(@PathVariable Long chatId, @Valid @RequestBody ImChatMemberBo bo) {
        return R.ok(chatService.addMember(chatId, bo));
    }

    /**
     * 移除聊天成员。
     *
     * <p>管理员可移除其他成员，普通成员可调用该接口退出聊天。</p>
     *
     * @param chatId 聊天 ID
     * @param userId 用户 ID
     * @return 操作结果响应
     */
    @SaCheckPermission("im:chat:member")
    @DeleteMapping("/{chatId}/members/{userId}")
    public R<Void> removeMember(@PathVariable Long chatId, @PathVariable Long userId) {
        chatService.removeMember(chatId, userId);
        return R.ok();
    }

    /**
     * 修改聊天成员角色。
     *
     * <p>仅 owner/admin 可调用，用于把群成员设为管理员或调整频道订阅者角色。</p>
     *
     * @param chatId 聊天 ID
     * @param userId 用户 ID
     * @param bo 成员角色入参
     * @return 成员视图响应
     */
    @SaCheckPermission("im:chat:member")
    @PutMapping("/{chatId}/members/{userId}")
    public R<ImChatMemberVo> updateMember(@PathVariable Long chatId,
                                          @PathVariable Long userId,
                                          @Valid @RequestBody ImChatMemberBo bo) {
        return R.ok(chatService.updateMember(chatId, userId, bo));
    }

    /**
     * 查询聊天置顶消息列表。
     *
     * <p>返回聊天内所有置顶记录，并补齐对应消息详情。</p>
     *
     * @param chatId 聊天 ID
     * @return 置顶消息列表响应
     */
    @SaCheckPermission("im:message:read")
    @GetMapping("/{chatId}/pinned")
    public R<List<ImPinnedMessageVo>> pinnedMessages(@PathVariable Long chatId) {
        return R.ok(pinnedMessageService.listPinned(chatId));
    }
}
