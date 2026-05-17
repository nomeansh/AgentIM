package com.AgentIM.business.im.service;

import com.AgentIM.business.im.domain.bo.ImContactCreateBo;
import com.AgentIM.business.im.domain.vo.ImContactVo;

import java.util.List;

/**
 * 联系人服务。
 */
public interface IImContactService {

    /**
     * 查询当前用户联系人列表。
     *
     * <p>联系人列表只返回当前用户主动添加且未删除的联系人，联系人关系不会影响已有聊天记录。</p>
     *
     * @return 联系人列表
     */
    List<ImContactVo> listContacts();

    /**
     * 添加或恢复联系人。
     *
     * <p>联系人关系是单向的。若当前用户已添加过该联系人，则更新备注并恢复正常状态；若不存在
     * 则创建新关系。</p>
     *
     * @param bo 添加联系人入参
     * @return 联系人视图
     */
    ImContactVo addContact(ImContactCreateBo bo);

    /**
     * 删除当前用户的联系人关系。
     *
     * <p>删除只影响通讯录展示，不删除用户资料，也不删除双方已有私聊和历史消息。</p>
     *
     * @param contactUserId 联系人用户 ID
     */
    void removeContact(Long contactUserId);
}
