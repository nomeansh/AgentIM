package com.AgentIM.business.im.mapper;

import com.AgentIM.business.im.domain.entity.ImMessage;
import com.AgentIM.business.im.domain.vo.ImMessageVo;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;

import java.util.List;

/**
 * 消息数据访问层。
 */
public interface ImMessageMapper extends BaseMapperPlus<ImMessage, ImMessageVo> {

    /**
     * 根据聊天和幂等键查询已有消息。
     *
     * <p>客户端重试发送时，Service 层先调用该方法。如果已经存在同一幂等键的消息，则直接返回
     * 原消息，保证重复请求不会生成多条消息。</p>
     *
     * @param chatId 聊天 ID
     * @param idempotentKey 幂等键
     * @return 已存在的消息，不存在时返回 null
     */
    @Select("""
        SELECT *
        FROM im_message
        WHERE chat_id = #{chatId}
          AND idempotent_key = #{idempotentKey}
          AND del_flag = '0'
        LIMIT 1
        """)
    ImMessage selectByIdempotentKey(@Param("chatId") Long chatId, @Param("idempotentKey") String idempotentKey);

    /**
     * 按游标查询当前用户可见的聊天消息。
     *
     * <p>查询按 seq 倒序返回，支持 beforeSeq 游标向历史翻页，并过滤对所有人删除和当前用户仅自己
     * 隐藏的消息。</p>
     *
     * @param chatId 聊天 ID
     * @param userId 当前用户 ID
     * @param beforeSeq 查询该序号之前的消息，null 表示最新消息
     * @param limit 最大返回条数
     * @return 消息视图列表
     */
    @Select("""
        <script>
        SELECT m.id,
               m.chat_id AS chatId,
               m.sender_id AS senderId,
               u.username AS senderUsername,
               u.nickname AS senderNickname,
               m.message_type AS messageType,
               m.content,
               m.content_payload AS contentPayload,
               m.resource_ids AS resourceIds,
               m.reply_to_message_id AS replyToMessageId,
               m.forward_from_message_id AS forwardFromMessageId,
               m.forward_from_chat_id AS forwardFromChatId,
               m.forward_sender_id AS forwardSenderId,
               m.client_msg_id AS clientMsgId,
               m.idempotent_key AS idempotentKey,
               m.seq,
               m.reply_count AS replyCount,
               m.status,
               m.edit_time AS editTime,
               m.delete_time AS deleteTime,
               m.create_time AS createTime
        FROM im_message m
        LEFT JOIN im_user u ON u.id = m.sender_id AND u.del_flag = '0'
        LEFT JOIN im_user_message_hide h
          ON h.message_id = m.id
         AND h.user_id = #{userId}
         AND h.del_flag = '0'
        WHERE m.chat_id = #{chatId}
          AND m.status <> 'deleted_all'
          AND m.del_flag = '0'
          AND h.id IS NULL
        <if test="beforeSeq != null">
          AND m.seq &lt; #{beforeSeq}
        </if>
        ORDER BY m.seq DESC
        LIMIT #{limit}
        </script>
        """)
    List<ImMessageVo> selectVisibleMessages(@Param("chatId") Long chatId,
                                            @Param("userId") Long userId,
                                            @Param("beforeSeq") Long beforeSeq,
                                            @Param("limit") int limit);

    /**
     * 查询当前用户在某个聊天中的未读消息数。
     *
     * <p>未读数通过消息 seq 与用户已读游标比较得出，同时排除对所有人删除和仅自己隐藏的消息。</p>
     *
     * @param chatId 聊天 ID
     * @param userId 当前用户 ID
     * @return 未读消息数量
     */
    @Select("""
        SELECT COUNT(1)
        FROM im_message m
        LEFT JOIN im_user_message_hide h
          ON h.message_id = m.id
         AND h.user_id = #{userId}
         AND h.del_flag = '0'
        WHERE m.chat_id = #{chatId}
          AND m.status <> 'deleted_all'
          AND m.del_flag = '0'
          AND h.id IS NULL
          AND m.seq > COALESCE((
              SELECT rs.last_read_seq
              FROM im_message_read_state rs
              WHERE rs.chat_id = #{chatId}
                AND rs.user_id = #{userId}
                AND rs.del_flag = '0'
              LIMIT 1
          ), 0)
        """)
    Long countUnread(@Param("chatId") Long chatId, @Param("userId") Long userId);

    /**
     * 更新消息回复计数。
     *
     * <p>发送引用回复消息后调用该方法增加原消息 replyCount，便于消息列表快速显示回复数量。</p>
     *
     * @param messageId 被回复的原消息 ID
     * @return 受影响行数
     */
    @Update("""
        UPDATE im_message
        SET reply_count = COALESCE(reply_count, 0) + 1,
            update_time = NOW()
        WHERE id = #{messageId}
          AND del_flag = '0'
        """)
    int incrementReplyCount(@Param("messageId") Long messageId);

    /**
     * 数据库兜底搜索当前用户可见消息。
     *
     * <p>P0 文档主张 ES 搜索；在 ES 尚未接入或本地开发时，该查询提供可运行的 PostgreSQL ILIKE
     * 兜底实现，仍然遵守成员可见性和消息删除过滤。</p>
     *
     * @param userId 当前用户 ID
     * @param chatId 限定聊天 ID，null 表示搜索所有可见聊天
     * @param keyword 搜索关键词
     * @param limit 最大返回条数
     * @return 消息视图列表
     */
    @Select("""
        <script>
        SELECT m.id,
               m.chat_id AS chatId,
               m.sender_id AS senderId,
               u.username AS senderUsername,
               u.nickname AS senderNickname,
               m.message_type AS messageType,
               m.content,
               m.content_payload AS contentPayload,
               m.resource_ids AS resourceIds,
               m.reply_to_message_id AS replyToMessageId,
               m.forward_from_message_id AS forwardFromMessageId,
               m.forward_from_chat_id AS forwardFromChatId,
               m.forward_sender_id AS forwardSenderId,
               m.client_msg_id AS clientMsgId,
               m.idempotent_key AS idempotentKey,
               m.seq,
               m.reply_count AS replyCount,
               m.status,
               m.edit_time AS editTime,
               m.delete_time AS deleteTime,
               m.create_time AS createTime
        FROM im_message m
        JOIN im_chat_member cm ON cm.chat_id = m.chat_id
        LEFT JOIN im_user u ON u.id = m.sender_id AND u.del_flag = '0'
        LEFT JOIN im_user_message_hide h
          ON h.message_id = m.id
         AND h.user_id = #{userId}
         AND h.del_flag = '0'
        WHERE cm.user_id = #{userId}
          AND cm.del_flag = '0'
          AND m.status IN ('normal', 'edited')
          AND m.del_flag = '0'
          AND h.id IS NULL
          AND m.content ILIKE CONCAT('%', #{keyword}, '%')
        <if test="chatId != null">
          AND m.chat_id = #{chatId}
        </if>
        ORDER BY m.create_time DESC
        LIMIT #{limit}
        </script>
        """)
    List<ImMessageVo> searchVisibleMessages(@Param("userId") Long userId,
                                            @Param("chatId") Long chatId,
                                            @Param("keyword") String keyword,
                                            @Param("limit") int limit);
}
