package com.AgentIM.business.im.domain.bo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 添加 Emoji 反应入参。
 *
 * <p>同一用户、同一消息、同一 reaction 会保持幂等，重复提交不会创建重复记录。</p>
 */
@Data
public class ImReactionBo {

    /**
     * Emoji 或反应标识。
     */
    @NotBlank(message = "反应标识不能为空")
    @Size(max = 50, message = "反应标识长度不能超过 50")
    private String reaction;
}
