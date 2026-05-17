import { afterEach, describe, expect, it } from "vitest";
import { defaultSettings, loadSettings } from "./session";

describe("连接设置", () => {
  afterEach(() => {
    localStorage.clear();
  });

  it("默认拆分认证服务和 IM 业务服务地址", () => {
    expect(defaultSettings.authBaseUrl).toBe("http://localhost:9210");
    expect(defaultSettings.apiBaseUrl).toBe("http://localhost:9211");
    expect(defaultSettings.wsPath).toBe("/ws/im");
  });

  it("兼容旧版 baseUrl 设置并迁移到两个后端地址", () => {
    localStorage.setItem(
      "agentim.settings",
      JSON.stringify({ baseUrl: "http://localhost:8080", wsPath: "/ws/im", clientId: "agentim-web", tenantId: "000000" })
    );

    expect(loadSettings()).toMatchObject({
      authBaseUrl: "http://localhost:8080",
      apiBaseUrl: "http://localhost:8080"
    });
  });
});
