package com.AgentIM.business.im.mapper;

import com.AgentIM.business.im.domain.entity.ImChat;
import com.AgentIM.business.im.domain.vo.ImChatVo;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;

import java.util.List;

/**
 * 聊天会话数据访问层。
 */
public interface ImChatMapper extends BaseMapperPlus<ImChat, ImChatVo> {

    /**
     * 查询两个用户之间已存在的私聊。
     *
     * <p>私聊必须且只能有两个成员。该查询通过两个成员存在性和成员总数同时约束，防止错误复用
     * 被污染的私聊会话。</p>
     *
     * @param userId1 用户一 ID
     * @param userId2 用户二 ID
     * @return 已存在的私聊会话，不存在时返回 null
     */
    @Select("""
        SELECT c.*
        FROM im_chat c
        WHERE c.type = 'private'
          AND c.status <> 'deleted'
          AND c.del_flag = '0'
          AND EXISTS (
              SELECT 1 FROM im_chat_member m1
              WHERE m1.chat_id = c.id AND m1.user_id = #{userId1} AND m1.del_flag = '0'
          )
          AND EXISTS (
              SELECT 1 FROM im_chat_member m2
              WHERE m2.chat_id = c.id AND m2.user_id = #{userId2} AND m2.del_flag = '0'
          )
          AND (
              SELECT COUNT(1) FROM im_chat_member mc
              WHERE mc.chat_id = c.id AND mc.del_flag = '0'
          ) = 2
        LIMIT 1
        """)
    ImChat selectPrivateChat(@Param("userId1") Long userId1, @Param("userId2") Long userId2);

    /**
     * 查询用户参与的活跃聊天列表。
     *
     * <p>查询会附带基于已读游标计算的未读数，并按最后消息时间倒序排列。归档聊天不会出现在主
     * 列表中，后续可按产品需要增加归档列表接口。</p>
     *
     * @param userId 当前用户 ID
     * @return 聊天视图列表
     */
    @Select("""
        SELECT c.id,
               c.type,
               c.title,
               c.avatar,
               c.description,
               c.owner_id AS ownerId,
               c.seq,
               c.last_msg_id AS lastMsgId,
               c.last_msg_content AS lastMsgContent,
               c.last_msg_time AS lastMsgTime,
               c.status,
               (
                   SELECT COUNT(1)
                   FROM im_message msg
                   LEFT JOIN im_user_message_hide h
                     ON h.message_id = msg.id
                    AND h.user_id = #{userId}
                    AND h.del_flag = '0'
                   WHERE msg.chat_id = c.id
                     AND msg.status <> 'deleted_all'
                     AND msg.del_flag = '0'
                     AND h.id IS NULL
                     AND msg.seq > COALESCE((
                         SELECT rs.last_read_seq
                         FROM im_message_read_state rs
                         WHERE rs.chat_id = c.id
                           AND rs.user_id = #{userId}
                           AND rs.del_flag = '0'
                         LIMIT 1
                     ), 0)
               ) AS unreadCount
        FROM im_chat c
        JOIN im_chat_member cm ON cm.chat_id = c.id
        WHERE cm.user_id = #{userId}
          AND cm.del_flag = '0'
          AND c.status = 'active'
          AND c.del_flag = '0'
        ORDER BY c.last_msg_time DESC NULLS LAST, c.update_time DESC NULLS LAST, c.create_time DESC
        """)
    List<ImChatVo> selectUserChats(@Param("userId") Long userId);

    /**
     * 原子递增聊天消息序号并返回新序号。
     *
     * <p>P0 以数据库中的 chat.seq 作为聊天内消息顺序源。该语句使用 PostgreSQL
     * UPDATE ... RETURNING 保证并发发送时不同事务拿到不同的递增序号。</p>
     *
     * @param chatId 聊天 ID
     * @return 递增后的聊天内序号
     */
    @Select("""
        UPDATE im_chat
        SET seq = COALESCE(seq, 0) + 1,
            update_time = NOW()
        WHERE id = #{chatId}
          AND del_flag = '0'
        RETURNING seq
        """)
    Long nextSeq(@Param("chatId") Long chatId);

    /**
     * 更新聊天最后消息摘要。
     *
     * <p>消息发送事务内调用该方法同步维护聊天列表展示所需字段，包括最新 seq、消息 ID、预览文本
     * 和最后消息时间。</p>
     *
     * @param chatId 聊天 ID
     * @param seq 最新消息序号
     * @param messageId 最新消息 ID
     * @param preview 最新消息预览
     * @return 受影响行数
     */
    @Update("""
        UPDATE im_chat
        SET seq = #{seq},
            last_msg_id = #{messageId},
            last_msg_content = #{preview},
            last_msg_time = NOW(),
            update_time = NOW()
        WHERE id = #{chatId}
          AND del_flag = '0'
        """)
    int updateLastMessage(@Param("chatId") Long chatId,
                          @Param("seq") Long seq,
                          @Param("messageId") Long messageId,
                          @Param("preview") String preview);
}
