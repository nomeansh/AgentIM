package com.AgentIM.business.im.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 文件资源访问级别。
 *
 * <p>访问级别决定资源下载时的二次鉴权策略。P0 默认使用 chat，表示只有关联聊天成员可访问。</p>
 */
@Getter
@AllArgsConstructor
public enum ImResourceAccessLevel implements ImCodeEnum {

    PRIVATE("private", "仅上传者可见"),
    CHAT("chat", "聊天成员可见"),
    PUBLIC("public", "所有人可见");

    private final String code;
    private final String desc;
}
