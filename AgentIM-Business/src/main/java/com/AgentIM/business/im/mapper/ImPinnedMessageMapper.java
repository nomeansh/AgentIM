package com.AgentIM.business.im.mapper;

import com.AgentIM.business.im.domain.entity.ImPinnedMessage;
import com.AgentIM.business.im.domain.vo.ImPinnedMessageVo;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;

import java.util.List;

/**
 * 置顶消息数据访问层。
 */
public interface ImPinnedMessageMapper extends BaseMapperPlus<ImPinnedMessage, ImPinnedMessageVo> {

    /**
     * 查询聊天内置顶消息列表。
     *
     * <p>结果按置顶时间倒序排列，Controller 会进一步补齐消息详情，供客户端在聊天顶部展示。</p>
     *
     * @param chatId 聊天 ID
     * @return 置顶记录列表
     */
    @Select("""
        SELECT id,
               chat_id AS chatId,
               message_id AS messageId,
               pinned_by AS pinnedBy,
               pinned_time AS pinnedTime
        FROM im_pinned_message
        WHERE chat_id = #{chatId}
          AND del_flag = '0'
        ORDER BY pinned_time DESC
        """)
    List<ImPinnedMessageVo> selectByChatId(@Param("chatId") Long chatId);
}
