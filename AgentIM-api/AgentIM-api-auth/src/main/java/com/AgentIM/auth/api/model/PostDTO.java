package com.AgentIM.auth.api.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 岗位信息。
 */
@Data
@NoArgsConstructor
public class PostDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 岗位 ID
     */
    private Long postId;

    /**
     * 部门 ID
     */
    private Long deptId;

    /**
     * 岗位编码
     */
    private String postCode;

    /**
     * 岗位名称
     */
    private String postName;

    /**
     * 岗位类别编码
     */
    private String postCategory;

}
