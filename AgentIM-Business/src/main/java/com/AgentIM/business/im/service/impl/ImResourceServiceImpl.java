package com.AgentIM.business.im.service.impl;

import com.AgentIM.business.im.constant.ImConstants;
import com.AgentIM.business.im.domain.entity.ImResource;
import com.AgentIM.business.im.domain.vo.ImResourceUploadVo;
import com.AgentIM.business.im.domain.vo.ImResourceVo;
import com.AgentIM.business.im.enums.ImResourceAccessLevel;
import com.AgentIM.business.im.enums.ImResourceType;
import com.AgentIM.business.im.mapper.ImResourceMapper;
import com.AgentIM.business.im.permission.ImPermissionService;
import com.AgentIM.business.im.service.IImResourceService;
import com.AgentIM.business.im.support.ImAuditService;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboReference;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.resource.api.RemoteFileService;
import org.dromara.resource.api.domain.RemoteFile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * IM 文件资源服务实现。
 */
@Service
@RequiredArgsConstructor
public class ImResourceServiceImpl implements IImResourceService {

    private final ImResourceMapper resourceMapper;
    private final ImPermissionService permissionService;
    private final ImAuditService auditService;

    @DubboReference(check = false)
    private RemoteFileService remoteFileService;

    /**
     * 上传文件并创建 IM 资源元信息。
     *
     * <p>外部资源服务上传在数据库写入之前执行，不放入事务方法中。若文件服务上传失败，IM 资源表
     * 不会产生孤儿记录。</p>
     *
     * @param file 上传文件
     * @return 上传结果
     */
    @Override
    public ImResourceUploadVo upload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ServiceException("上传文件不能为空");
        }
        Long userId = permissionService.currentUserId();
        RemoteFile remoteFile = uploadRemote(file);
        ImResource resource = new ImResource();
        resource.setUploaderId(userId);
        resource.setResourceType(resolveType(file.getContentType()).getCode());
        resource.setOriginalName(file.getOriginalFilename());
        resource.setMimeType(file.getContentType());
        resource.setSize(file.getSize());
        resource.setStorageProvider("oss");
        resource.setObjectKey(String.valueOf(remoteFile.getOssId()));
        resource.setUrl(remoteFile.getUrl());
        resource.setAccessLevel(ImResourceAccessLevel.CHAT.getCode());
        resource.setDelFlag(ImConstants.DEL_FLAG_NORMAL);
        resourceMapper.insert(resource);
        auditService.record(null, "resource", resource.getId(), "UPLOAD_RESOURCE", userId,
            "上传文件 " + resource.getOriginalName(), resource);

        ImResourceUploadVo vo = new ImResourceUploadVo();
        vo.setResourceId(resource.getId());
        vo.setOriginalName(resource.getOriginalName());
        vo.setUrl(resource.getUrl());
        vo.setThumbnailUrl(resource.getThumbnailKey());
        return vo;
    }

    /**
     * 查询资源元信息。
     *
     * <p>方法会按资源访问级别执行二次鉴权，防止用户通过资源 ID 枚举下载其他聊天文件。</p>
     *
     * @param resourceId 资源 ID
     * @return 资源视图
     */
    @Override
    public ImResourceVo getResource(Long resourceId) {
        ImResource resource = requireResource(resourceId);
        checkReadable(resource);
        return MapstructUtils.convert(resource, ImResourceVo.class);
    }

    /**
     * 获取资源下载地址。
     *
     * <p>P0 返回资源服务保存的 URL。生产环境如需短期签名 URL，可在该方法内部替换实现。</p>
     *
     * @param resourceId 资源 ID
     * @return 可访问下载地址
     */
    @Override
    public String downloadUrl(Long resourceId) {
        return getResource(resourceId).getUrl();
    }

    /**
     * 调用远程资源服务上传文件。
     *
     * <p>该方法封装 MultipartFile 到字节数组的读取和 Dubbo 调用异常，将底层 IOException 转换为
     * 业务异常。</p>
     *
     * @param file 上传文件
     * @return 远程文件信息
     */
    private RemoteFile uploadRemote(MultipartFile file) {
        try {
            if (remoteFileService == null) {
                throw new ServiceException("资源服务未配置");
            }
            return remoteFileService.upload(file.getName(), file.getOriginalFilename(), file.getContentType(), file.getBytes());
        } catch (IOException e) {
            throw new ServiceException("读取上传文件失败");
        }
    }

    /**
     * 查询资源并要求存在。
     *
     * <p>资源不存在时抛出业务异常，避免下载接口返回空 URL。</p>
     *
     * @param resourceId 资源 ID
     * @return 资源实体
     */
    private ImResource requireResource(Long resourceId) {
        ImResource resource = resourceMapper.selectById(resourceId);
        if (resource == null) {
            throw new ServiceException("资源不存在");
        }
        return resource;
    }

    /**
     * 校验资源读取权限。
     *
     * <p>public 资源直接放行；private 资源仅上传者可见；chat 资源在绑定聊天后要求当前用户是聊天
     * 成员，未绑定前仅上传者可见。</p>
     *
     * @param resource 资源实体
     */
    private void checkReadable(ImResource resource) {
        Long userId = permissionService.currentUserId();
        if (ImResourceAccessLevel.PUBLIC.getCode().equals(resource.getAccessLevel())) {
            return;
        }
        if (ImResourceAccessLevel.PRIVATE.getCode().equals(resource.getAccessLevel())) {
            if (!userId.equals(resource.getUploaderId())) {
                throw new ServiceException("无权访问该资源");
            }
            return;
        }
        if (resource.getChatId() == null) {
            if (!userId.equals(resource.getUploaderId())) {
                throw new ServiceException("无权访问未绑定资源");
            }
            return;
        }
        permissionService.requireReadable(resource.getChatId());
    }

    /**
     * 根据 MIME 类型解析资源类型。
     *
     * <p>图片、音频、视频有专门类型，其余文件归为 file 或 other，用于消息发送时的媒体校验。</p>
     *
     * @param mimeType MIME 类型
     * @return 资源类型
     */
    private ImResourceType resolveType(String mimeType) {
        if (mimeType == null) {
            return ImResourceType.OTHER;
        }
        if (mimeType.startsWith("image/")) {
            return ImResourceType.IMAGE;
        }
        if (mimeType.startsWith("audio/")) {
            return ImResourceType.VOICE;
        }
        if (mimeType.startsWith("video/")) {
            return ImResourceType.VIDEO;
        }
        return ImResourceType.FILE;
    }
}
