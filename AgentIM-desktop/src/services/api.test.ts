import { afterEach, describe, expect, it, vi } from "vitest";
import { AgentImApi } from "./api";
import type { LoginRequest } from "../types/im";

describe("AgentImApi 服务地址路由", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("登录注册请求使用认证地址，IM 请求使用业务地址", async () => {
    const urls: string[] = [];
    vi.stubGlobal(
      "fetch",
      vi.fn(async (input: RequestInfo | URL) => {
        urls.push(String(input));
        return new Response(JSON.stringify({ code: 200, msg: "ok", data: { access_token: "token", id: "u1", username: "alice" } }), {
          status: 200
        });
      })
    );

    const api = new AgentImApi(
      { authBaseUrl: "http://localhost:9210", apiBaseUrl: "http://localhost:9211", clientId: "agentim-web" },
      () => "token"
    );
    const login: LoginRequest = { clientId: "agentim-web", grantType: "password", username: "alice", password: "secret" };

    await api.login(login);
    await api.register({ ...login, userType: "app_user" });
    await api.profile();

    expect(urls).toEqual(["http://localhost:9210/login", "http://localhost:9210/register", "http://localhost:9211/im/users/profile"]);
  });
});
