package com.AgentIM.business.im.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.mybatis.core.domain.BaseEntity;

/**
 * IM 文件资源元信息实体。
 *
 * <p>该表只记录对象存储元信息和聊天关联，不保存文件内容。下载时必须根据 accessLevel、上传者
 * 和聊天成员关系重新鉴权。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("im_resource")
public class ImResource extends BaseEntity {

    /**
     * 资源 ID。
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 关联聊天 ID。
     */
    private Long chatId;

    /**
     * 关联消息 ID。
     */
    private Long messageId;

    /**
     * 上传者用户 ID。
     */
    private Long uploaderId;

    /**
     * 资源类型。
     */
    private String resourceType;

    /**
     * 原始文件名。
     */
    private String originalName;

    /**
     * MIME 类型。
     */
    private String mimeType;

    /**
     * 文件大小，单位字节。
     */
    private Long size;

    /**
     * 存储服务商。
     */
    private String storageProvider;

    /**
     * 对象存储键或 OSS ID。
     */
    private String objectKey;

    /**
     * 缩略图对象键。
     */
    private String thumbnailKey;

    /**
     * 图片或视频宽度。
     */
    private Integer width;

    /**
     * 图片或视频高度。
     */
    private Integer height;

    /**
     * 音频或视频时长，单位秒。
     */
    private Integer duration;

    /**
     * 访问级别。
     */
    private String accessLevel;

    /**
     * 可访问 URL。
     */
    private String url;

    /**
     * 逻辑删除标记。
     */
    @TableLogic(value = "0", delval = "2")
    private String delFlag;
}
