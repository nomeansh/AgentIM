package com.AgentIM.business.im.mapper;

import com.AgentIM.business.im.domain.entity.ImPoll;
import com.AgentIM.business.im.domain.vo.ImPollVo;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;

/**
 * 投票数据访问层。
 */
public interface ImPollMapper extends BaseMapperPlus<ImPoll, ImPollVo> {

    /**
     * 根据消息 ID 查询投票。
     *
     * <p>投票消息详情和投票提交都需要先从消息定位投票主体。不存在时说明消息不是合法投票消息。</p>
     *
     * @param messageId 消息 ID
     * @return 投票视图，不存在时返回 null
     */
    @Select("""
        SELECT id,
               message_id AS messageId,
               question,
               multiple,
               anonymous,
               status,
               close_time AS closeTime
        FROM im_poll
        WHERE message_id = #{messageId}
          AND del_flag = '0'
        LIMIT 1
        """)
    ImPollVo selectVoByMessageId(@Param("messageId") Long messageId);
}
