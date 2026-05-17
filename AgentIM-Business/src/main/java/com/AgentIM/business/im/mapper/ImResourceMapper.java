package com.AgentIM.business.im.mapper;

import com.AgentIM.business.im.domain.entity.ImResource;
import com.AgentIM.business.im.domain.vo.ImResourceVo;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;

import java.util.List;

/**
 * IM 文件资源数据访问层。
 */
public interface ImResourceMapper extends BaseMapperPlus<ImResource, ImResourceVo> {

    /**
     * 统计当前用户可以绑定到指定消息的资源数。
     *
     * <p>发送/编辑消息只能绑定当前用户上传的资源，并且资源必须尚未绑定或已绑定到当前消息，避免
     * 通过猜测资源 ID 把其他用户或其他消息的资源挪到新消息上。</p>
     *
     * @param chatId 聊天 ID
     * @param messageId 消息 ID
     * @param userId 当前用户 ID
     * @param resourceIds 资源 ID 列表
     * @return 可绑定资源数量
     */
    @Select("""
        <script>
        SELECT COUNT(DISTINCT id)
        FROM im_resource
        WHERE id IN
        <foreach collection="resourceIds" item="resourceId" open="(" separator="," close=")">
            #{resourceId}
        </foreach>
          AND uploader_id = #{userId}
          AND del_flag = '0'
          AND (chat_id IS NULL OR chat_id = #{chatId})
          AND (message_id IS NULL OR message_id = #{messageId})
        </script>
        """)
    int countBindableResources(@Param("chatId") Long chatId,
                               @Param("messageId") Long messageId,
                               @Param("userId") Long userId,
                               @Param("resourceIds") List<Long> resourceIds);

    /**
     * 将资源关联到聊天和消息。
     *
     * <p>上传文件和发送消息是两个阶段。发送消息成功后调用该方法，把已上传的资源绑定到具体聊天
     * 与消息，下载鉴权即可基于聊天成员关系判断。</p>
     *
     * @param chatId 聊天 ID
     * @param messageId 消息 ID
     * @param userId 当前用户 ID
     * @param resourceIds 资源 ID 列表
     * @return 受影响行数
     */
    @Update("""
        <script>
        UPDATE im_resource
        SET chat_id = #{chatId},
            message_id = #{messageId},
            update_time = NOW()
        WHERE id IN
        <foreach collection="resourceIds" item="resourceId" open="(" separator="," close=")">
            #{resourceId}
        </foreach>
          AND uploader_id = #{userId}
          AND del_flag = '0'
          AND (chat_id IS NULL OR chat_id = #{chatId})
          AND (message_id IS NULL OR message_id = #{messageId})
        </script>
        """)
    int bindToMessage(@Param("chatId") Long chatId,
                      @Param("messageId") Long messageId,
                      @Param("userId") Long userId,
                      @Param("resourceIds") List<Long> resourceIds);
}
