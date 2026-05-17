package com.AgentIM.business.im.service;

import com.AgentIM.business.im.domain.bo.ImReactionBo;
import com.AgentIM.business.im.domain.vo.ImReactionVo;

import java.util.List;

/**
 * 消息反应服务。
 */
public interface IImReactionService {

    /**
     * 查询消息反应明细。
     *
     * <p>调用前会校验当前用户对消息所属聊天有读取权限。</p>
     *
     * @param messageId 消息 ID
     * @return 反应明细列表
     */
    List<ImReactionVo> listReactions(Long messageId);

    /**
     * 添加 Emoji 反应。
     *
     * <p>同一用户对同一消息的同一反应保持幂等，重复添加会直接返回已有记录。</p>
     *
     * @param messageId 消息 ID
     * @param bo 反应入参
     * @return 反应视图
     */
    ImReactionVo addReaction(Long messageId, ImReactionBo bo);

    /**
     * 删除 Emoji 反应。
     *
     * <p>普通用户只能删除自己的反应，owner/admin 可以删除聊天中任意用户的反应。</p>
     *
     * @param messageId 消息 ID
     * @param userId 反应所属用户 ID
     * @param reaction 反应标识
     */
    void deleteReaction(Long messageId, Long userId, String reaction);
}
