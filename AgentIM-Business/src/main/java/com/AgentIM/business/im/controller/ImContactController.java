package com.AgentIM.business.im.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.AgentIM.business.im.domain.bo.ImContactCreateBo;
import com.AgentIM.business.im.domain.vo.ImContactVo;
import com.AgentIM.business.im.service.IImContactService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * IM 联系人接口。
 *
 * <p>联系人模型采用 Telegram 式单向通讯录关系，添加和删除联系人不会影响已有聊天。</p>
 */
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/im/contacts")
public class ImContactController {

    private final IImContactService contactService;

    /**
     * 查询当前用户联系人列表。
     *
     * <p>仅返回当前用户主动添加且未删除的联系人。</p>
     *
     * @return 联系人列表响应
     */
    @SaCheckPermission("im:contact:list")
    @GetMapping
    public R<List<ImContactVo>> listContacts() {
        return R.ok(contactService.listContacts());
    }

    /**
     * 添加联系人。
     *
     * <p>重复添加会恢复或更新现有关系，保持接口幂等。</p>
     *
     * @param bo 添加联系人入参
     * @return 联系人视图响应
     */
    @SaCheckPermission("im:contact:add")
    @PostMapping
    public R<ImContactVo> addContact(@Valid @RequestBody ImContactCreateBo bo) {
        return R.ok(contactService.addContact(bo));
    }

    /**
     * 删除联系人。
     *
     * <p>删除只影响当前用户通讯录展示，不删除聊天和消息历史。</p>
     *
     * @param contactUserId 联系人用户 ID
     * @return 操作结果响应
     */
    @SaCheckPermission("im:contact:remove")
    @DeleteMapping("/{contactUserId}")
    public R<Void> removeContact(@PathVariable Long contactUserId) {
        contactService.removeContact(contactUserId);
        return R.ok();
    }
}
