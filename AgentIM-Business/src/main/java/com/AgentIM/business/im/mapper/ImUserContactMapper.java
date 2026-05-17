package com.AgentIM.business.im.mapper;

import com.AgentIM.business.im.domain.entity.ImUserContact;
import com.AgentIM.business.im.domain.vo.ImContactVo;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;

import java.util.List;

/**
 * 用户联系人数据访问层。
 */
public interface ImUserContactMapper extends BaseMapperPlus<ImUserContact, ImContactVo> {

    /**
     * 查询当前用户的联系人列表。
     *
     * <p>该查询将联系人关系表和用户资料表关联，返回客户端展示需要的用户名、昵称、头像和备注。
     * 已删除关系和已删除用户都会被过滤。</p>
     *
     * @param userId 当前用户 ID
     * @return 联系人视图列表
     */
    @Select("""
        SELECT c.id,
               c.contact_user_id AS contactUserId,
               u.username,
               u.nickname,
               u.avatar,
               c.remark,
               c.status
        FROM im_user_contact c
        JOIN im_user u ON u.id = c.contact_user_id AND u.del_flag = '0'
        WHERE c.user_id = #{userId}
          AND c.status = '0'
          AND c.del_flag = '0'
        ORDER BY COALESCE(c.update_time, c.create_time) DESC
        """)
    List<ImContactVo> selectContacts(@Param("userId") Long userId);
}
