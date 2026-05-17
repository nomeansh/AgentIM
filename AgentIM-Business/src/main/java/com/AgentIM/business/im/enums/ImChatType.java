package com.AgentIM.business.im.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.dromara.common.core.exception.ServiceException;

import java.util.Arrays;

/**
 * 聊天会话类型。
 *
 * <p>AgentIM 将私聊、群组、频道和保存的消息统一建模为聊天容器，通过该枚举区分成员上限、
 * 发言权限和系统行为。</p>
 */
@Getter
@AllArgsConstructor
public enum ImChatType implements ImCodeEnum {

    PRIVATE("private", "私聊"),
    GROUP("group", "群组"),
    CHANNEL("channel", "频道"),
    SAVED("saved", "保存的消息");

    private final String code;
    private final String desc;

    /**
     * 根据接口或数据库中的编码解析聊天类型。
     *
     * <p>该方法是创建聊天、校验频道发言和保存消息访问控制的统一入口。编码为空或未知时直接
     * 抛出业务异常，避免无效类型进入数据库后破坏权限判断。</p>
     *
     * @param code 待解析的聊天类型编码
     * @return 匹配的聊天类型枚举
     */
    public static ImChatType require(String code) {
        return Arrays.stream(values())
            .filter(item -> item.code.equals(code))
            .findFirst()
            .orElseThrow(() -> new ServiceException("不支持的聊天类型: {}", code));
    }

    /**
     * 判断编码是否属于频道类型。
     *
     * <p>发送消息和置顶消息时需要对频道做额外角色校验，该方法让调用处不必直接比较字符串。</p>
     *
     * @param code 待判断的聊天类型编码
     * @return true 表示编码为频道，false 表示其他聊天类型
     */
    public static boolean isChannel(String code) {
        return CHANNEL.code.equals(code);
    }
}
