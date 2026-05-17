package com.AgentIM.business.im.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 文件资源类型。
 *
 * <p>资源类型用于区分下载鉴权、缩略图展示和消息类型校验。文件真实内容存储在 OSS/MinIO/S3，
 * IM 数据库仅记录元信息和对象键。</p>
 */
@Getter
@AllArgsConstructor
public enum ImResourceType implements ImCodeEnum {

    FILE("file", "文件"),
    IMAGE("image", "图片"),
    VOICE("voice", "语音"),
    VIDEO("video", "视频"),
    OTHER("other", "其他");

    private final String code;
    private final String desc;
}
