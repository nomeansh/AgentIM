package com.AgentIM.business.im.service;

import com.AgentIM.business.im.domain.vo.ImSearchResultVo;

/**
 * IM 搜索服务。
 */
public interface IImSearchService {

    /**
     * 搜索当前用户可见消息和用户资料。
     *
     * <p>P0 优先提供数据库兜底实现，遵守聊天成员可见性。后续接入 ES 时可保持接口契约不变。</p>
     *
     * @param keyword 搜索关键词
     * @param chatId 限定聊天 ID，null 表示全局搜索
     * @param limit 最大返回条数
     * @return 搜索结果
     */
    ImSearchResultVo search(String keyword, Long chatId, int limit);
}
