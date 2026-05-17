package com.AgentIM.business.im.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.Size;
import lombok.Data;
import com.AgentIM.business.im.domain.entity.ImUser;

/**
 * 当前用户资料更新入参。
 *
 * <p>该对象只允许修改 IM 展示资料，不承载密码、登录状态和权限字段，避免资料接口绕过认证
 * 服务修改敏感账号信息。</p>
 */
@Data
@AutoMapper(target = ImUser.class, reverseConvertGenerate = false)
public class ImUserProfileBo {

    /**
     * 展示昵称。
     */
    @Size(max = 50, message = "昵称长度不能超过 50")
    private String nickname;

    /**
     * 头像 URL 或资源标识。
     */
    @Size(max = 500, message = "头像地址长度不能超过 500")
    private String avatar;

    /**
     * 个人简介。
     */
    @Size(max = 200, message = "个人简介长度不能超过 200")
    private String bio;

    /**
     * 手机号。
     */
    @Size(max = 20, message = "手机号长度不能超过 20")
    private String phone;

    /**
     * 邮箱。
     */
    @Size(max = 100, message = "邮箱长度不能超过 100")
    private String email;
}
