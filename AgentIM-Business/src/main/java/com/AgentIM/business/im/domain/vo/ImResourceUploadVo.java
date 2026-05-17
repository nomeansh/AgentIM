package com.AgentIM.business.im.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 文件上传结果视图对象。
 */
@Data
public class ImResourceUploadVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long resourceId;
    private String originalName;
    private String url;
    private String thumbnailUrl;
}
