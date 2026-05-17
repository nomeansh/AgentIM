package com.AgentIM.business.im.domain.bo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 用户级数据访问策略更新入参。
 *
 * <p>该对象只作为非聊天数据权限的扩展预留，不替代 im_chat_member 的聊天成员权限。</p>
 */
@Data
public class ImUserAccessPolicyBo {

    /**
     * 用户级数据访问范围。
     */
    @NotBlank(message = "数据访问范围不能为空")
    @Pattern(regexp = "standard|restricted|trusted|blocked", message = "数据访问范围只能是 standard、restricted、trusted、blocked")
    private String dataScope;

    /**
     * 用户权限标签 JSON 数组字符串。
     */
    @NotBlank(message = "权限标签不能为空")
    @Size(max = 2000, message = "权限标签长度不能超过 2000")
    private String permissionTags;

    /**
     * 用户级访问策略 JSON 对象字符串。
     */
    @NotBlank(message = "访问策略不能为空")
    @Size(max = 4000, message = "访问策略长度不能超过 4000")
    private String accessPolicy;
}
