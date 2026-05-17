package com.AgentIM.business.im.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import com.AgentIM.business.im.domain.entity.ImResource;

import java.io.Serial;
import java.io.Serializable;

/**
 * 文件资源视图对象。
 */
@Data
@AutoMapper(target = ImResource.class)
public class ImResourceVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long chatId;
    private Long messageId;
    private Long uploaderId;
    private String resourceType;
    private String originalName;
    private String mimeType;
    private Long size;
    private String storageProvider;
    private String objectKey;
    private String thumbnailKey;
    private Integer width;
    private Integer height;
    private Integer duration;
    private String accessLevel;
    private String url;
}
