package com.AgentIM.business.im.mapper;

import com.AgentIM.business.im.domain.entity.ImMessageReaction;
import com.AgentIM.business.im.domain.vo.ImReactionSummaryVo;
import com.AgentIM.business.im.domain.vo.ImReactionVo;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;

import java.util.List;

/**
 * 消息反应数据访问层。
 */
public interface ImMessageReactionMapper extends BaseMapperPlus<ImMessageReaction, ImReactionVo> {

    /**
     * 查询某条消息的反应明细。
     *
     * <p>该查询关联用户资料，便于客户端展示每个反应来自哪个用户。</p>
     *
     * @param messageId 消息 ID
     * @return 反应明细列表
     */
    @Select("""
        SELECT r.id,
               r.message_id AS messageId,
               r.user_id AS userId,
               u.username,
               u.nickname,
               r.reaction
        FROM im_message_reaction r
        JOIN im_user u ON u.id = r.user_id AND u.del_flag = '0'
        WHERE r.message_id = #{messageId}
          AND r.del_flag = '0'
        ORDER BY r.create_time ASC
        """)
    List<ImReactionVo> selectByMessageId(@Param("messageId") Long messageId);

    /**
     * 查询某条消息的反应聚合结果。
     *
     * <p>消息列表需要快速展示每种 Emoji 的数量，该查询按 reaction 分组统计，避免客户端自行聚合
     * 明细列表。</p>
     *
     * @param messageId 消息 ID
     * @return 反应聚合列表
     */
    @Select("""
        SELECT reaction,
               COUNT(1) AS count
        FROM im_message_reaction
        WHERE message_id = #{messageId}
          AND del_flag = '0'
        GROUP BY reaction
        ORDER BY count DESC, reaction ASC
        """)
    List<ImReactionSummaryVo> selectSummaryByMessageId(@Param("messageId") Long messageId);
}
