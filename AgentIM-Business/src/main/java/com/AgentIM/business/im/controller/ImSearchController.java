package com.AgentIM.business.im.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.AgentIM.business.im.domain.vo.ImSearchResultVo;
import com.AgentIM.business.im.service.IImSearchService;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * IM 搜索接口。
 *
 * <p>P0 后端明确使用 PostgreSQL 数据库搜索，当前阶段暂不接入 Elasticsearch。</p>
 */
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/im/search")
public class ImSearchController {

    private final IImSearchService searchService;

    /**
     * 搜索当前用户可见消息和用户资料。
     *
     * <p>chatId 为空时搜索当前用户所有可见聊天；传入 chatId 时仅搜索该聊天。</p>
     *
     * @param q 搜索关键词
     * @param chatId 限定聊天 ID
     * @param limit 最大返回条数
     * @return 搜索结果响应
     */
    @SaCheckPermission("im:message:read")
    @GetMapping
    public R<ImSearchResultVo> search(@RequestParam("q") String q,
                                      @RequestParam(value = "chatId", required = false) Long chatId,
                                      @RequestParam(value = "limit", defaultValue = "20") int limit) {
        return R.ok(searchService.search(q, chatId, limit));
    }
}
