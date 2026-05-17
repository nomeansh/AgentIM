package org.dromara.common.websocket.interceptor;

import cn.dev33.satoken.exception.NotLoginException;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.satoken.utils.LoginHelper;
import com.AgentIM.auth.api.model.LoginUser;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.util.MultiValueMap;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

import static org.dromara.common.websocket.constant.WebSocketConstants.LOGIN_USER_KEY;

/**
 * WebSocket 握手请求拦截器。
 * <p>
 * 用于在建立 WebSocket 会话前解析当前登录用户，并把登录用户写入会话属性，供后续消息处理器识别连接归属。
 * 由于浏览器和 Tauri 前端在 WebSocket 握手时不能稳定附带自定义 Authorization 请求头，本拦截器同时兼容
 * Sa-Token 当前上下文、URL 查询参数 token、URL 查询参数 Authorization 和请求头 Authorization。
 *
 * @author zendwang
 */
@Slf4j
public class PlusWebSocketInterceptor implements HandshakeInterceptor {

    /**
     * WebSocket 握手前置处理。
     * <p>
     * 握手阶段解析登录用户，解析成功时把用户对象写入 {@link org.dromara.common.websocket.constant.WebSocketConstants#LOGIN_USER_KEY}；
     * 解析失败时拒绝本次握手，避免未认证连接进入业务消息通道。
     *
     * @param request    WebSocket 握手请求
     * @param response   WebSocket 握手响应
     * @param wsHandler  WebSocket 处理程序
     * @param attributes 与 WebSocket 会话关联的属性
     * @return 允许握手继续时返回 {@code true}；拒绝握手时返回 {@code false}
     */
    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Map<String, Object> attributes) {
        try {
            LoginUser loginUser = resolveLoginUser(request);
            if (loginUser == null) {
                log.error("WebSocket 认证失败，无法根据握手请求解析登录用户");
                return false;
            }
            attributes.put(LOGIN_USER_KEY, loginUser);
            return true;
        } catch (NotLoginException e) {
            log.error("WebSocket 认证失败：{}，无法访问系统资源", e.getMessage());
            return false;
        }
    }

    /**
     * WebSocket 握手成功后的后置处理。
     * <p>
     * 当前业务不需要额外处理，保留该方法以满足 {@link HandshakeInterceptor} 协议并为后续审计日志或指标埋点预留扩展点。
     *
     * @param request   WebSocket 握手请求
     * @param response  WebSocket 握手响应
     * @param wsHandler WebSocket 处理程序
     * @param exception 握手过程中可能出现的异常
     */
    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Exception exception) {
        // 当前无需额外处理；后续如需记录握手审计日志，可在这里扩展。
    }

    /**
     * 解析 WebSocket 握手请求对应的登录用户。
     * <p>
     * 解析顺序：
     * <ol>
     *     <li>优先读取 Sa-Token 当前上下文，兼容能正常传递认证上下文的服务端场景。</li>
     *     <li>当前上下文未登录时，继续从握手请求里的 token 或 Authorization 中提取令牌。</li>
     *     <li>拿到令牌后通过 Sa-Token 令牌会话反查 {@link LoginUser}。</li>
     * </ol>
     *
     * @param request WebSocket 握手请求
     * @return 解析到的登录用户；无法解析时返回 {@code null}
     */
    private LoginUser resolveLoginUser(ServerHttpRequest request) {
        LoginUser loginUser = null;
        try {
            loginUser = LoginHelper.getLoginUser();
        } catch (NotLoginException ignored) {
            // 浏览器和 Tauri 前端不能稳定为 WebSocket 握手设置自定义 Authorization 请求头，因此继续尝试握手参数中的令牌。
        }
        if (loginUser != null) {
            return loginUser;
        }
        String token = resolveToken(request);
        return StrUtil.isBlank(token) ? null : LoginHelper.getLoginUser(token);
    }

    /**
     * 从 WebSocket 握手请求中提取认证令牌。
     * <p>
     * 前端统一使用 {@code /ws/im?token=<access_token>}，这里同时兼容历史调用可能携带的
     * {@code Authorization} 查询参数或请求头，并去除可选的 {@code Bearer } 前缀。
     *
     * @param request WebSocket 握手请求
     * @return 已去除 {@code Bearer } 前缀的令牌；不存在时返回空字符串
     */
    private String resolveToken(ServerHttpRequest request) {
        MultiValueMap<String, String> params = UriComponentsBuilder.fromUri(request.getURI()).build().getQueryParams();
        String token = params.getFirst("token");
        if (StrUtil.isBlank(token)) {
            token = params.getFirst("Authorization");
        }
        if (StrUtil.isBlank(token)) {
            token = request.getHeaders().getFirst("Authorization");
        }
        return StrUtil.removePrefixIgnoreCase(StrUtil.trimToEmpty(token), "Bearer ");
    }

}
