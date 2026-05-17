package com.AgentIM.business.im.enums;

/**
 * 带编码和值描述的 IM 枚举通用契约。
 *
 * <p>P0 的数据库枚举值统一使用小写 snake_case 字符串。该接口让权限校验、参数标准化和
 * 业务展示可以使用一致的读取方式，同时避免每个枚举重复定义同名访问方法。</p>
 */
public interface ImCodeEnum {

    /**
     * 获取写入数据库和接口响应的稳定编码。
     *
     * <p>编码是后端与客户端、数据库之间的契约，不应随展示文案变化。Service 层做参数校验时
     * 也以该编码为准。</p>
     *
     * @return 小写 snake_case 格式的业务编码
     */
    String getCode();

    /**
     * 获取用于日志、审计和接口文档说明的人类可读描述。
     *
     * <p>描述仅用于展示和排障，不参与持久化约束判断。</p>
     *
     * @return 中文业务描述
     */
    String getDesc();
}
