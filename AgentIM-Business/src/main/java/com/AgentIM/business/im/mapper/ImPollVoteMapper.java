package com.AgentIM.business.im.mapper;

import com.AgentIM.business.im.domain.entity.ImPollVote;
import com.AgentIM.business.im.domain.vo.ImPollVo;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;

/**
 * 投票记录数据访问层。
 */
public interface ImPollVoteMapper extends BaseMapperPlus<ImPollVote, ImPollVo> {

    /**
     * 清理用户在某个投票下的旧投票记录。
     *
     * <p>单选投票重新选择时调用该方法逻辑删除旧选择，再写入新选择，保证同一用户只保留一个有效
     * 选项。</p>
     *
     * @param pollId 投票 ID
     * @param userId 用户 ID
     * @return 受影响行数
     */
    @Update("""
        UPDATE im_poll_vote
        SET del_flag = '2',
            update_time = NOW()
        WHERE poll_id = #{pollId}
          AND user_id = #{userId}
          AND del_flag = '0'
        """)
    int deleteUserVotes(@Param("pollId") Long pollId, @Param("userId") Long userId);
}
