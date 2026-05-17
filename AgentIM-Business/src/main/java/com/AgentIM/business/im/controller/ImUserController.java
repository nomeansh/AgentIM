package com.AgentIM.business.im.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.AgentIM.business.im.domain.bo.ImUserAccessPolicyBo;
import com.AgentIM.business.im.domain.bo.ImUserProfileBo;
import com.AgentIM.business.im.domain.vo.ImUserAccessPolicyVo;
import com.AgentIM.business.im.domain.vo.ImUserVo;
import com.AgentIM.business.im.service.IImUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * IM 用户资料接口。
 *
 * <p>提供当前用户资料、公开资料和用户搜索能力。账号注册登录仍走 Auth 服务。</p>
 */
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/im/users")
public class ImUserController {

    private final IImUserService userService;

    /**
     * 获取当前登录用户资料。
     *
     * <p>接口从 Sa-Token 上下文识别当前用户，返回 IM 展示资料，不包含密码摘要和权限集合。</p>
     *
     * @return 当前用户资料响应
     */
    @SaCheckPermission("im:user:profile")
    @GetMapping("/profile")
    public R<ImUserVo> profile() {
        return R.ok(userService.getProfile());
    }

    /**
     * 更新当前登录用户资料。
     *
     * <p>只允许更新昵称、头像、简介、电话和邮箱等展示字段，不能通过该接口修改账号密码或状态。</p>
     *
     * @param bo 资料更新入参
     * @return 更新后的用户资料响应
     */
    @SaCheckPermission("im:user:profile")
    @PutMapping("/profile")
    public R<ImUserVo> updateProfile(@Valid @RequestBody ImUserProfileBo bo) {
        return R.ok(userService.updateProfile(bo));
    }

    /**
     * 查询用户级数据访问策略。
     *
     * <p>该接口是非聊天数据权限扩展预留面，普通用户资料接口不会返回这些策略字段。</p>
     *
     * @param userId 用户 ID
     * @return 用户访问策略响应
     */
    @SaCheckPermission("im:user:access-policy")
    @GetMapping("/{userId}/access-policy")
    public R<ImUserAccessPolicyVo> getAccessPolicy(@PathVariable Long userId) {
        return R.ok(userService.getAccessPolicy(userId));
    }

    /**
     * 更新用户级数据访问策略。
     *
     * <p>该接口只写入 im_user 的预留策略字段，不改变聊天成员关系和当前 P0 聊天权限判断。</p>
     *
     * @param userId 用户 ID
     * @param bo 策略更新入参
     * @return 更新后的用户访问策略响应
     */
    @SaCheckPermission("im:user:access-policy")
    @PutMapping("/{userId}/access-policy")
    public R<ImUserAccessPolicyVo> updateAccessPolicy(@PathVariable Long userId,
                                                      @Valid @RequestBody ImUserAccessPolicyBo bo) {
        return R.ok(userService.updateAccessPolicy(userId, bo));
    }

    /**
     * 查询指定用户公开资料。
     *
     * <p>用于联系人、成员列表和用户资料卡展示。返回内容不包含敏感账号字段。</p>
     *
     * @param userId 用户 ID
     * @return 用户公开资料响应
     */
    @SaCheckPermission("im:user:profile")
    @GetMapping("/{userId}")
    public R<ImUserVo> getUser(@PathVariable Long userId) {
        return R.ok(userService.getUser(userId));
    }

    /**
     * 搜索用户。
     *
     * <p>按用户名和昵称模糊搜索，limit 默认 20 且由服务层收敛最大值。</p>
     *
     * @param q 搜索关键词
     * @param limit 最大返回条数
     * @return 用户搜索结果响应
     */
    @SaCheckPermission("im:user:profile")
    @GetMapping("/search")
    public R<List<ImUserVo>> search(@RequestParam("q") String q,
                                    @RequestParam(value = "limit", defaultValue = "20") int limit) {
        return R.ok(userService.searchUsers(q, limit));
    }
}
