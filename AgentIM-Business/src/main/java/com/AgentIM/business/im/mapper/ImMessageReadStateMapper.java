package com.AgentIM.business.im.mapper;

import com.AgentIM.business.im.domain.entity.ImMessageReadState;
import com.AgentIM.business.im.domain.vo.ImMessageReadStatusVo;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;

/**
 * 消息已读游标数据访问层。
 */
public interface ImMessageReadStateMapper extends BaseMapperPlus<ImMessageReadState, ImMessageReadStatusVo> {

    /**
     * 查询某用户在某聊天中的已读游标。
     *
     * <p>Service 层用该方法实现“只前进不后退”的游标更新逻辑，避免较旧设备覆盖较新已读位置。</p>
     *
     * @param userId 用户 ID
     * @param chatId 聊天 ID
     * @return 已读游标，不存在时返回 null
     */
    @Select("""
        SELECT *
        FROM im_message_read_state
        WHERE user_id = #{userId}
          AND chat_id = #{chatId}
          AND del_flag = '0'
        LIMIT 1
        """)
    ImMessageReadState selectByUserAndChat(@Param("userId") Long userId, @Param("chatId") Long chatId);

    /**
     * 判断用户是否已经读到指定聊天序号。
     *
     * <p>私聊三态对勾和群聊已读状态计算都依赖该判断。只要 lastReadSeq 大于等于消息 seq，就认为
     * 该用户已经阅读该消息。</p>
     *
     * @param userId 用户 ID
     * @param chatId 聊天 ID
     * @param seq 消息序号
     * @return true 表示已读，false 表示未读
     */
    @Select("""
        SELECT EXISTS (
            SELECT 1
            FROM im_message_read_state
            WHERE user_id = #{userId}
              AND chat_id = #{chatId}
              AND last_read_seq >= #{seq}
              AND del_flag = '0'
        )
        """)
    Boolean isRead(@Param("userId") Long userId, @Param("chatId") Long chatId, @Param("seq") Long seq);
}
