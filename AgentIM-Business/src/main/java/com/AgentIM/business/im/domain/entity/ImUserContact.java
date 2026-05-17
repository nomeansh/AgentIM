package com.AgentIM.business.im.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.mybatis.core.domain.BaseEntity;

/**
 * 用户联系人实体。
 *
 * <p>联系人关系按单向边保存，A 添加 B 只代表 A 的通讯录中出现 B。删除联系人不会影响历史
 * 聊天和消息记录。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("im_user_contact")
public class ImUserContact extends BaseEntity {

    /**
     * 关系主键。
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 联系人拥有者用户 ID。
     */
    private Long userId;

    /**
     * 被添加为联系人的用户 ID。
     */
    private Long contactUserId;

    /**
     * 备注名。
     */
    private String remark;

    /**
     * 关系状态，0 表示正常，1 表示用户主动移除。
     */
    private String status;

    /**
     * 逻辑删除标记。
     */
    @TableLogic(value = "0", delval = "2")
    private String delFlag;
}
