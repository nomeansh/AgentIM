package com.AgentIM.business.im.domain.bo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

/**
 * IM 用户注册入参。
 *
 * <p>认证服务调用 Business Dubbo 契约注册用户时使用该对象，注册成功后会同步创建用户资料和
 * 保存的消息会话。</p>
 */
@Data
public class ImRegisterBo {

    /**
     * 用户名 @handle。
     */
    @NotBlank(message = "用户名不能为空")
    @Length(min = 2, max = 50, message = "用户名长度必须在 2 到 50 之间")
    private String username;

    /**
     * 明文密码，仅在注册事务中转换为摘要，不落库保存。
     */
    @NotBlank(message = "密码不能为空")
    @Length(min = 5, max = 100, message = "密码长度必须在 5 到 100 之间")
    private String password;

    /**
     * 用户类型，默认 app_user。
     */
    private String userType;
}
