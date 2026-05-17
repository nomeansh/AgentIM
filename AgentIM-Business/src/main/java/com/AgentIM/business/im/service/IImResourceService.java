package com.AgentIM.business.im.service;

import com.AgentIM.business.im.domain.vo.ImResourceUploadVo;
import com.AgentIM.business.im.domain.vo.ImResourceVo;
import org.springframework.web.multipart.MultipartFile;

/**
 * IM 文件资源服务。
 */
public interface IImResourceService {

    /**
     * 上传文件并创建 IM 资源元信息。
     *
     * <p>文件内容交给资源微服务处理，Business 只保存 IM 访问控制所需的元信息。资源在发送消息时
     * 再绑定到聊天和消息。</p>
     *
     * @param file 上传文件
     * @return 上传结果
     */
    ImResourceUploadVo upload(MultipartFile file);

    /**
     * 查询资源元信息。
     *
     * <p>私有资源只允许上传者查看，聊天资源要求当前用户是关联聊天成员，公开资源可直接查看。</p>
     *
     * @param resourceId 资源 ID
     * @return 资源视图
     */
    ImResourceVo getResource(Long resourceId);

    /**
     * 获取资源下载地址。
     *
     * <p>P0 复用资源服务返回的 URL。正式部署时可替换为带有效期的临时下载地址。</p>
     *
     * @param resourceId 资源 ID
     * @return 可访问下载地址
     */
    String downloadUrl(Long resourceId);
}
