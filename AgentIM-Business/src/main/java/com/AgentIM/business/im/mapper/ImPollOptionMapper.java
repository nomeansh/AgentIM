package com.AgentIM.business.im.mapper;

import com.AgentIM.business.im.domain.entity.ImPollOption;
import com.AgentIM.business.im.domain.vo.ImPollOptionVo;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;

import java.util.List;

/**
 * 投票选项数据访问层。
 */
public interface ImPollOptionMapper extends BaseMapperPlus<ImPollOption, ImPollOptionVo> {

    /**
     * 查询投票选项及票数。
     *
     * <p>返回值包含每个选项的总票数，以及当前用户是否选择该选项，便于客户端直接渲染投票结果。</p>
     *
     * @param pollId 投票 ID
     * @param userId 当前用户 ID
     * @return 投票选项视图列表
     */
    @Select("""
        SELECT o.id,
               o.poll_id AS pollId,
               o.text,
               o.ordinal,
               COUNT(v.id) AS voteCount,
               EXISTS (
                   SELECT 1 FROM im_poll_vote mv
                   WHERE mv.poll_id = o.poll_id
                     AND mv.option_id = o.id
                     AND mv.user_id = #{userId}
                     AND mv.del_flag = '0'
               ) AS selectedByMe
        FROM im_poll_option o
        LEFT JOIN im_poll_vote v
          ON v.option_id = o.id
         AND v.del_flag = '0'
        WHERE o.poll_id = #{pollId}
          AND o.del_flag = '0'
        GROUP BY o.id, o.poll_id, o.text, o.ordinal
        ORDER BY o.ordinal ASC
        """)
    List<ImPollOptionVo> selectOptions(@Param("pollId") Long pollId, @Param("userId") Long userId);
}
