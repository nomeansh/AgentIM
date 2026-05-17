package com.AgentIM.business.im.domain.bo;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 添加联系人入参。
 *
 * <p>联系人关系是单向的，当前登录用户作为 userId，被添加用户作为 contactUserId。重复添加时
 * 服务层会恢复或保持现有关系。</p>
 */
@Data
public class ImContactCreateBo {

    /**
     * 被添加为联系人的用户 ID。
     */
    @NotNull(message = "联系人用户ID不能为空")
    private Long contactUserId;

    /**
     * 备注名。
     */
    @Size(max = 100, message = "联系人备注长度不能超过 100")
    private String remark;
}
