import { describe, expect, it } from "vitest";
import { deriveWsUrl, parseSocketMessage } from "./ws";

describe("WebSocket 契约适配", () => {
  it("从 HTTP 基础地址派生 /ws/im token 地址", () => {
    expect(deriveWsUrl("http://localhost:9211", "abc 123")).toBe("ws://localhost:9211/ws/im?token=abc+123");
  });

  it("兼容后端直接推送 IM 事件 JSON", () => {
    const event = parseSocketMessage(
      JSON.stringify({
        eventId: "evt_1",
        eventType: "message.created",
        chatId: "10",
        actorId: "20",
        payload: { id: "30" },
        occurredAt: "2026-05-16 10:00:00"
      })
    );

    expect(event?.eventType).toBe("message.created");
    expect(event?.payload).toEqual({ id: "30" });
  });

  it("兼容 WebSocketMessageDto 包装消息", () => {
    const event = parseSocketMessage(
      JSON.stringify({
        sessionKeys: ["1"],
        message: JSON.stringify({ eventType: "chat.updated", payload: { id: "10" } })
      })
    );

    expect(event?.eventType).toBe("chat.updated");
  });
});
