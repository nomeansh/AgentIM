package com.AgentIM.business.im.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.AgentIM.business.im.domain.vo.ImResourceUploadVo;
import com.AgentIM.business.im.domain.vo.ImResourceVo;
import com.AgentIM.business.im.service.IImResourceService;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * IM 文件资源接口。
 *
 * <p>资源接口只负责后端上传、元信息查询和下载地址获取，不实现任何前端上传组件。</p>
 */
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/im/resources")
public class ImResourceController {

    private final IImResourceService resourceService;

    /**
     * 上传文件。
     *
     * <p>文件内容交给资源服务保存，Business 侧创建 IM 资源元信息并返回 resourceId，客户端随后在
     * 发送媒体消息时引用该 ID。</p>
     *
     * @param file 上传文件
     * @return 上传结果响应
     */
    @SaCheckPermission("im:resource:upload")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public R<ImResourceUploadVo> upload(@RequestPart("file") MultipartFile file) {
        return R.ok(resourceService.upload(file));
    }

    /**
     * 查询资源元信息。
     *
     * <p>服务层会根据 accessLevel、上传者和聊天成员关系执行下载前鉴权。</p>
     *
     * @param resourceId 资源 ID
     * @return 资源元信息响应
     */
    @SaCheckPermission("im:message:read")
    @GetMapping("/{resourceId}")
    public R<ImResourceVo> getResource(@PathVariable Long resourceId) {
        return R.ok(resourceService.getResource(resourceId));
    }

    /**
     * 获取资源下载地址。
     *
     * <p>P0 返回资源服务 URL；生产环境可替换为短期签名 URL 或重定向响应。</p>
     *
     * @param resourceId 资源 ID
     * @return 下载地址响应
     */
    @SaCheckPermission("im:message:read")
    @GetMapping("/{resourceId}/download")
    public R<String> download(@PathVariable Long resourceId) {
        return R.ok(resourceService.downloadUrl(resourceId));
    }
}
