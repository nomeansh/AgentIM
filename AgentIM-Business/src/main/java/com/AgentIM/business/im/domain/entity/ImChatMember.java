package com.AgentIM.business.im.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.mybatis.core.domain.BaseEntity;

import java.util.Date;

/**
 * 聊天成员实体。
 *
 * <p>该表是所有聊天数据权限的核心依据。任何读取、发送、成员管理、置顶、已读等操作都必须先
 * 确认当前用户在目标聊天中有有效成员关系。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("im_chat_member")
public class ImChatMember extends BaseEntity {

    /**
     * 成员关系主键。
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 聊天 ID。
     */
    private Long chatId;

    /**
     * 用户 ID。
     */
    private Long userId;

    /**
     * 成员角色：owner、admin、member、subscriber。
     */
    private String role;

    /**
     * 加入聊天时间。
     */
    private Date joinedTime;

    /**
     * 是否免打扰，0 表示正常，1 表示免打扰。
     */
    private String muted;

    /**
     * 逻辑删除标记。
     */
    @TableLogic(value = "0", delval = "2")
    private String delFlag;
}
