import { describe, expect, it } from "vitest";
import { buildQuery, normalizeBaseUrl, unwrapResponse } from "./http";

describe("http 契约适配", () => {
  it("解包 R<T> 成功响应", () => {
    expect(unwrapResponse({ code: 200, msg: "操作成功", data: { id: "1" } })).toEqual({ id: "1" });
  });

  it("R<T> 非 200 时抛出后端 msg", () => {
    expect(() => unwrapResponse({ code: 403, msg: "无访问权限", data: null })).toThrow("无访问权限");
  });

  it("构造查询串时忽略空值并保留 0", () => {
    expect(buildQuery({ q: "alice", chatId: undefined, limit: 0, blank: "" })).toBe("?q=alice&limit=0");
  });

  it("规范化基础地址时移除末尾斜杠", () => {
    expect(normalizeBaseUrl("http://localhost:9211///")).toBe("http://localhost:9211");
  });
});
