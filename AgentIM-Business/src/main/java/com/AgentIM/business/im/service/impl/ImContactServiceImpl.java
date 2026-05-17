package com.AgentIM.business.im.service.impl;

import com.AgentIM.business.im.constant.ImConstants;
import com.AgentIM.business.im.domain.bo.ImContactCreateBo;
import com.AgentIM.business.im.domain.entity.ImUserContact;
import com.AgentIM.business.im.domain.vo.ImContactVo;
import com.AgentIM.business.im.mapper.ImUserContactMapper;
import com.AgentIM.business.im.permission.ImPermissionService;
import com.AgentIM.business.im.service.IImContactService;
import com.AgentIM.business.im.service.IImUserService;
import com.AgentIM.business.im.support.ImAuditService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.exception.ServiceException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 联系人服务实现。
 */
@Service
@RequiredArgsConstructor
public class ImContactServiceImpl implements IImContactService {

    private final ImUserContactMapper contactMapper;
    private final IImUserService userService;
    private final ImPermissionService permissionService;
    private final ImAuditService auditService;

    /**
     * 查询当前用户联系人列表。
     *
     * <p>联系人列表由 Mapper 关联用户资料后返回，Service 只负责读取当前用户 ID。</p>
     *
     * @return 联系人列表
     */
    @Override
    public List<ImContactVo> listContacts() {
        return contactMapper.selectContacts(permissionService.currentUserId());
    }

    /**
     * 添加或恢复联系人。
     *
     * <p>不允许把自己加为联系人。若关系已存在，更新备注并恢复正常状态；否则插入新关系。</p>
     *
     * @param bo 添加联系人入参
     * @return 联系人视图
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ImContactVo addContact(ImContactCreateBo bo) {
        Long userId = permissionService.currentUserId();
        if (userId.equals(bo.getContactUserId())) {
            throw new ServiceException("不能添加自己为联系人");
        }
        userService.requireUser(bo.getContactUserId());
        ImUserContact contact = contactMapper.selectOne(new LambdaQueryWrapper<ImUserContact>()
            .eq(ImUserContact::getUserId, userId)
            .eq(ImUserContact::getContactUserId, bo.getContactUserId())
            .last("LIMIT 1"));
        if (contact == null) {
            contact = new ImUserContact();
            contact.setUserId(userId);
            contact.setContactUserId(bo.getContactUserId());
            contact.setDelFlag(ImConstants.DEL_FLAG_NORMAL);
        }
        contact.setRemark(bo.getRemark());
        contact.setStatus("0");
        if (contact.getId() == null) {
            contactMapper.insert(contact);
        } else {
            contactMapper.updateById(contact);
        }
        auditService.record(null, "contact", contact.getId(), "ADD_CONTACT", userId,
            "添加联系人", bo);
        return contactMapper.selectContacts(userId).stream()
            .filter(item -> bo.getContactUserId().equals(item.getContactUserId()))
            .findFirst()
            .orElseThrow(() -> new ServiceException("联系人创建失败"));
    }

    /**
     * 删除当前用户的联系人关系。
     *
     * <p>关系不存在时保持幂等，不抛出错误。删除不会影响已有聊天和消息。</p>
     *
     * @param contactUserId 联系人用户 ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeContact(Long contactUserId) {
        Long userId = permissionService.currentUserId();
        ImUserContact contact = contactMapper.selectOne(new LambdaQueryWrapper<ImUserContact>()
            .eq(ImUserContact::getUserId, userId)
            .eq(ImUserContact::getContactUserId, contactUserId)
            .last("LIMIT 1"));
        if (contact == null) {
            return;
        }
        contact.setStatus("1");
        contactMapper.updateById(contact);
        auditService.record(null, "contact", contact.getId(), "REMOVE_CONTACT", userId,
            "删除联系人", contact);
    }
}
