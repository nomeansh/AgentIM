package com.AgentIM.business.im.support;

import com.AgentIM.business.im.constant.ImConstants;
import com.AgentIM.business.im.domain.entity.ImAuditLog;
import com.AgentIM.business.im.mapper.ImAuditLogMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.utils.ServletUtils;
import org.dromara.common.json.utils.JsonUtils;
import org.springframework.stereotype.Service;

import java.util.Date;

/**
 * IM 审计日志服务。
 *
 * <p>审计日志记录关键写操作的人、时间、资源和摘要。P0 使用追加写入，不在业务流程中更新或删除
 * 审计记录。</p>
 */
@Service
@RequiredArgsConstructor
public class ImAuditService {

    private final ImAuditLogMapper auditLogMapper;

    /**
     * 写入一条审计日志。
     *
     * <p>方法会尽量从当前 HTTP 请求中补充 IP 和 User-Agent；若当前调用来自 Dubbo 或后台任务，
     * 请求为空也不会影响审计写入。</p>
     *
     * @param chatId 关联聊天 ID，可为空
     * @param resourceType 资源类型
     * @param resourceId 资源 ID，可为空
     * @param action 操作编码
     * @param actorId 操作人 ID，可为空
     * @param summary 人类可读摘要
     * @param payload 结构化载荷
     */
    public void record(Long chatId, String resourceType, Long resourceId, String action,
                       Long actorId, String summary, Object payload) {
        ImAuditLog log = new ImAuditLog();
        log.setChatId(chatId);
        log.setResourceType(resourceType);
        log.setResourceId(resourceId);
        log.setAction(action);
        log.setActorId(actorId);
        log.setSummary(summary);
        log.setPayload(JsonUtils.toJsonString(payload));
        log.setOccurredTime(new Date());
        log.setDelFlag(ImConstants.DEL_FLAG_NORMAL);
        fillRequestInfo(log);
        auditLogMapper.insert(log);
    }

    /**
     * 从当前请求补充客户端信息。
     *
     * <p>该方法对缺失请求上下文容错，避免 Dubbo 调用或异步任务写审计时因为无法读取 request 而
     * 中断主业务事务。</p>
     *
     * @param log 待补充的审计日志实体
     */
    private void fillRequestInfo(ImAuditLog log) {
        HttpServletRequest request = ServletUtils.getRequest();
        if (request == null) {
            return;
        }
        log.setIpaddr(ServletUtils.getClientIP());
        log.setUserAgent(request.getHeader("User-Agent"));
    }
}
