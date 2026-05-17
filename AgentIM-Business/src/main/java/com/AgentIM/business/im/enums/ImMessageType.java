package com.AgentIM.business.im.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.dromara.common.core.exception.ServiceException;

import java.util.Arrays;
import java.util.Set;

/**
 * 消息类型。
 *
 * <p>消息类型决定 content、contentPayload 和 resourceIds 的校验规则。媒体消息只引用资源 ID，
 * 文件内容不进入消息表，符合 P0 对对象存储边界的要求。</p>
 */
@Getter
@AllArgsConstructor
public enum ImMessageType implements ImCodeEnum {

    TEXT("text", "文本"),
    IMAGE("image", "图片"),
    FILE("file", "文件"),
    VOICE("voice", "语音"),
    VIDEO("video", "视频"),
    POLL("poll", "投票"),
    SYSTEM("system", "系统消息"),
    FORWARD("forward", "转发消息");

    private static final Set<String> MEDIA_TYPES = Set.of(IMAGE.code, FILE.code, VOICE.code, VIDEO.code);

    private final String code;
    private final String desc;

    /**
     * 根据编码解析消息类型。
     *
     * <p>发送消息时通过该方法提前拒绝未知类型，确保消息表中的类型值可以被客户端和搜索索引
     * 稳定理解。</p>
     *
     * @param code 待解析的消息类型编码
     * @return 匹配的消息类型
     */
    public static ImMessageType require(String code) {
        return Arrays.stream(values())
            .filter(item -> item.code.equals(code))
            .findFirst()
            .orElseThrow(() -> new ServiceException("不支持的消息类型: {}", code));
    }

    /**
     * 判断消息类型是否必须携带资源引用。
     *
     * <p>图片、文件、语音、视频都需要先上传到资源服务，再在消息中引用 resourceId。该方法集中
     * 表达这条约束，防止各个业务入口重复写散落的类型判断。</p>
     *
     * @param code 待判断的消息类型编码
     * @return true 表示属于媒体消息并要求 resourceIds 非空
     */
    public static boolean isMedia(String code) {
        return MEDIA_TYPES.contains(code);
    }
}
