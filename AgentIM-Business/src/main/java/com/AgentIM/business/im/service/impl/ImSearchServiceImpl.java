package com.AgentIM.business.im.service.impl;

import com.AgentIM.business.im.domain.vo.ImSearchResultVo;
import com.AgentIM.business.im.mapper.ImMessageMapper;
import com.AgentIM.business.im.permission.ImPermissionService;
import com.AgentIM.business.im.service.IImSearchService;
import com.AgentIM.business.im.service.IImUserService;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.utils.StringUtils;
import org.springframework.stereotype.Service;

/**
 * IM 搜索服务实现。
 */
@Service
@RequiredArgsConstructor
public class ImSearchServiceImpl implements IImSearchService {

    private final ImMessageMapper messageMapper;
    private final IImUserService userService;
    private final ImPermissionService permissionService;

    /**
     * 搜索当前用户可见消息和用户资料。
     *
     * <p>当前实现使用 PostgreSQL 兜底搜索，仍严格限制消息搜索范围为当前用户参与的聊天。传入
     * chatId 时会先校验当前用户可读取该聊天。</p>
     *
     * @param keyword 搜索关键词
     * @param chatId 限定聊天 ID
     * @param limit 最大返回条数
     * @return 搜索结果
     */
    @Override
    public ImSearchResultVo search(String keyword, Long chatId, int limit) {
        ImSearchResultVo result = new ImSearchResultVo();
        if (StringUtils.isBlank(keyword)) {
            result.setMessages(java.util.List.of());
            result.setUsers(java.util.List.of());
            return result;
        }
        int realLimit = normalizeLimit(limit);
        if (chatId != null) {
            permissionService.requireReadable(chatId);
        }
        result.setMessages(messageMapper.searchVisibleMessages(permissionService.currentUserId(), chatId, keyword.trim(), realLimit));
        result.setUsers(userService.searchUsers(keyword, realLimit));
        return result;
    }

    /**
     * 收敛搜索返回条数。
     *
     * <p>默认返回 20 条，最大 50 条，防止搜索接口造成数据库压力。</p>
     *
     * @param limit 入参条数
     * @return 规范化条数
     */
    private int normalizeLimit(int limit) {
        if (limit <= 0) {
            return 20;
        }
        return Math.min(limit, 50);
    }
}
