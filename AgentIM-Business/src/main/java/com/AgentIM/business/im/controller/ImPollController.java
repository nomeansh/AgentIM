package com.AgentIM.business.im.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.AgentIM.business.im.domain.bo.ImPollVoteBo;
import com.AgentIM.business.im.domain.vo.ImPollVo;
import com.AgentIM.business.im.service.IImPollService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * IM 投票接口。
 *
 * <p>投票通过 poll 类型消息创建，本接口提供投票详情查询和投票提交能力。</p>
 */
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/im/messages/{messageId}/poll")
public class ImPollController {

    private final IImPollService pollService;

    /**
     * 查询消息关联的投票详情。
     *
     * <p>返回选项、票数和当前用户是否已选择。</p>
     *
     * @param messageId 消息 ID
     * @return 投票详情响应
     */
    @SaCheckPermission("im:message:read")
    @GetMapping
    public R<ImPollVo> getPoll(@PathVariable Long messageId) {
        return R.ok(pollService.getPoll(messageId));
    }

    /**
     * 提交投票。
     *
     * <p>单选投票会替换旧选择，多选投票允许保留多个选项。</p>
     *
     * @param messageId 消息 ID
     * @param bo 投票入参
     * @return 更新后的投票详情响应
     */
    @SaCheckPermission("im:message:send")
    @PostMapping("/vote")
    public R<ImPollVo> vote(@PathVariable Long messageId, @Valid @RequestBody ImPollVoteBo bo) {
        return R.ok(pollService.vote(messageId, bo));
    }
}
