package org.dromara.common.log.event;

import cn.hutool.http.useragent.UserAgent;
import cn.hutool.http.useragent.UserAgentUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.constant.Constants;
import org.dromara.common.core.utils.ServletUtils;
import org.dromara.common.core.utils.ip.AddressUtils;
import org.dromara.common.json.utils.JsonUtils;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 异步调用日志服务
 *
 * @author ruoyi
 */
@Component
@Slf4j
public class LogEventListener {

    /**
     * 保存系统日志记录
     */
    @EventListener
    public void saveLog(OperLogEvent operLogEvent) {
        log.info("操作日志: {}", JsonUtils.toJsonString(operLogEvent));
    }

    /**
     * 保存系统访问记录
     */
    @EventListener
    public void saveLogininfor(LogininforEvent logininforEvent) {
        HttpServletRequest request = ServletUtils.getRequest();
        final UserAgent userAgent = UserAgentUtil.parse(request.getHeader("User-Agent"));
        final String ip = ServletUtils.getClientIP(request);

        String address = AddressUtils.getRealAddressByIP(ip);
        StringBuilder s = new StringBuilder();
        s.append(getBlock(ip));
        s.append(address);
        s.append(getBlock(logininforEvent.getUsername()));
        s.append(getBlock(logininforEvent.getStatus()));
        s.append(getBlock(logininforEvent.getMessage()));
        // 打印信息到日志
        log.info(s.toString(), logininforEvent.getArgs());
        // 获取客户端操作系统
        String os = userAgent.getOs().getName();
        // 获取客户端浏览器
        String browser = userAgent.getBrowser().getName();
        log.info("访问日志: [{}][{}][{}][{}][{}]", logininforEvent.getUsername(), ip, address, browser, os);
    }

    private String getBlock(Object msg) {
        if (msg == null) {
            msg = "";
        }
        return "[" + msg + "]";
    }

}
