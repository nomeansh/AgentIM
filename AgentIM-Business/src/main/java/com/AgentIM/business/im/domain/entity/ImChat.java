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
 * 聊天会话实体。
 *
 * <p>私聊、群组、频道和保存的消息都落在同一张表，通过 type 字段区分行为。seq 字段是聊天内
 * 消息游标，所有消息分页和已读计算都以它为准。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("im_chat")
public class ImChat extends BaseEntity {

    /**
     * 聊天 ID。
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 聊天类型：private、group、channel、saved。
     */
    private String type;

    /**
     * 群组或频道标题。
     */
    private String title;

    /**
     * 群组或频道头像。
     */
    private String avatar;

    /**
     * 群组或频道简介。
     */
    private String description;

    /**
     * 创建者或频道所有者。
     */
    private Long ownerId;

    /**
     * 聊天内最新消息序号。
     */
    private Long seq;

    /**
     * 最后一条消息 ID。
     */
    private Long lastMsgId;

    /**
     * 最后一条消息预览文本。
     */
    private String lastMsgContent;

    /**
     * 最后一条消息时间。
     */
    private Date lastMsgTime;

    /**
     * 会话状态：active、archived、deleted。
     */
    private String status;

    /**
     * 逻辑删除标记。
     */
    @TableLogic(value = "0", delval = "2")
    private String delFlag;
}
