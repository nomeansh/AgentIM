package com.AgentIM.business.im.mapper;

import com.AgentIM.business.im.domain.entity.ImChatMember;
import com.AgentIM.business.im.domain.vo.ImChatMemberVo;
import com.AgentIM.business.im.domain.vo.ImReadMemberVo;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;

import java.util.List;

/**
 * 聊天成员数据访问层。
 */
public interface ImChatMemberMapper extends BaseMapperPlus<ImChatMember, ImChatMemberVo> {

    /**
     * 查询聊天成员用户 ID。
     *
     * <p>领域事件发布、权限广播和成员管理都需要使用该列表。查询只返回未删除的有效成员。</p>
     *
     * @param chatId 聊天 ID
     * @return 成员用户 ID 列表
     */
    @Select("""
        SELECT user_id
        FROM im_chat_member
        WHERE chat_id = #{chatId}
          AND del_flag = '0'
        ORDER BY joined_time ASC
        """)
    List<Long> selectUserIdsByChatId(@Param("chatId") Long chatId);

    /**
     * 查询聊天成员详情列表。
     *
     * <p>该查询关联用户资料，供聊天详情和成员列表接口展示成员身份、昵称和头像。</p>
     *
     * @param chatId 聊天 ID
     * @return 成员视图列表
     */
    @Select("""
        SELECT cm.id,
               cm.chat_id AS chatId,
               cm.user_id AS userId,
               u.username,
               u.nickname,
               u.avatar,
               cm.role,
               cm.joined_time AS joinedTime,
               cm.muted
        FROM im_chat_member cm
        JOIN im_user u ON u.id = cm.user_id AND u.del_flag = '0'
        WHERE cm.chat_id = #{chatId}
          AND cm.del_flag = '0'
        ORDER BY cm.joined_time ASC
        """)
    List<ImChatMemberVo> selectMembers(@Param("chatId") Long chatId);

    /**
     * 查询当前用户在聊天中的角色。
     *
     * <p>Service 层使用该方法做数据级权限判断。返回 null 表示用户不是有效成员。</p>
     *
     * @param chatId 聊天 ID
     * @param userId 用户 ID
     * @return 角色编码，不存在时返回 null
     */
    @Select("""
        SELECT role
        FROM im_chat_member
        WHERE chat_id = #{chatId}
          AND user_id = #{userId}
          AND del_flag = '0'
        LIMIT 1
        """)
    String selectRole(@Param("chatId") Long chatId, @Param("userId") Long userId);

    /**
     * 查询某条消息对应的所有成员已读状态。
     *
     * <p>该查询是飞书式已读模型的核心：成员游标 lastReadSeq 大于等于消息 seq 即表示已读。发送者
     * 默认排除在统计外。</p>
     *
     * @param messageId 消息 ID
     * @return 成员已读状态列表
     */
    @Select("""
        SELECT cm.user_id AS userId,
               u.username,
               u.nickname,
               u.avatar,
               rs.last_read_message_id AS lastReadMessageId,
               rs.last_read_seq AS lastReadSeq,
               rs.last_read_time AS readTime,
               CASE WHEN COALESCE(rs.last_read_seq, 0) >= m.seq THEN TRUE ELSE FALSE END AS hasRead
        FROM im_message m
        JOIN im_chat_member cm ON cm.chat_id = m.chat_id AND cm.del_flag = '0'
        JOIN im_user u ON u.id = cm.user_id AND u.del_flag = '0'
        LEFT JOIN im_message_read_state rs
          ON rs.chat_id = m.chat_id
         AND rs.user_id = cm.user_id
         AND rs.del_flag = '0'
        WHERE m.id = #{messageId}
          AND m.del_flag = '0'
          AND cm.user_id <> COALESCE(m.sender_id, 0)
        ORDER BY hasRead DESC, u.username ASC
        """)
    List<ImReadMemberVo> selectReadMembers(@Param("messageId") Long messageId);
}
