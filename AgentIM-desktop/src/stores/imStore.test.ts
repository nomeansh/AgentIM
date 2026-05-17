import { describe, expect, it } from "vitest";
import type { ImMessage } from "../types/im";
import { applyEventToState, createInitialState, insertOrReplaceMessage, sortMessagesAscending } from "./imStore";

describe("消息状态归并", () => {
  it("按 seq 升序展示消息", () => {
    const messages = [
      { id: "2", seq: "2", chatId: "1", senderId: "9", messageType: "text", status: "sent" },
      { id: "1", seq: "1", chatId: "1", senderId: "9", messageType: "text", status: "sent" }
    ] as ImMessage[];

    expect(sortMessagesAscending(messages).map((item) => item.id)).toEqual(["1", "2"]);
  });

  it("同一消息事件到达时替换旧消息而不是重复插入", () => {
    const next = insertOrReplaceMessage(
      [{ id: "1", content: "旧", seq: "1", chatId: "1", senderId: "9", messageType: "text", status: "sent" }] as ImMessage[],
      { id: "1", content: "新", seq: "1", chatId: "1", senderId: "9", messageType: "text", status: "edited" } as ImMessage
    );

    expect(next).toHaveLength(1);
    expect(next[0].content).toBe("新");
  });

  it("实时归并反应、置顶、隐藏和聊天删除事件", () => {
    const state = createInitialState();
    state.profile = { id: "9", username: "alice" };
    state.chats = [{ id: "1", type: "group", title: "研发群", unreadCount: 3 }];
    state.messagesByChat["1"] = [
      { id: "m1", chatId: "1", senderId: "8", messageType: "text", content: "hello", seq: "1", status: "sent", reactions: [{ reaction: "+1", count: 1 }] }
    ] as ImMessage[];

    applyEventToState(state, { eventType: "reaction.created", chatId: "1", payload: { messageId: "m1", reaction: "+1", userId: "9" } });
    expect(state.messagesByChat["1"][0].reactions).toEqual([{ reaction: "+1", count: 2 }]);

    applyEventToState(state, { eventType: "message.pinned", chatId: "1", payload: { id: "p1", chatId: "1", messageId: "m1" } });
    expect(state.pinnedByChat["1"]).toHaveLength(1);

    applyEventToState(state, { eventType: "read_state.updated", chatId: "1", actorId: "9", payload: { chatId: "1" } });
    expect(state.chats[0].unreadCount).toBe(0);

    applyEventToState(state, { eventType: "message.hidden", chatId: "1", payload: { messageId: "m1", chatId: "1" } });
    expect(state.messagesByChat["1"]).toHaveLength(0);

    applyEventToState(state, { eventType: "chat.deleted", chatId: "1", payload: { id: "1" } });
    expect(state.chats).toHaveLength(0);
  });

  it("chat.updated 携带成员载荷时不应误写入伪聊天", () => {
    const state = createInitialState();
    state.chats = [{ id: "1", type: "group", title: "研发群" }];

    applyEventToState(state, {
      eventType: "chat.updated",
      chatId: "1",
      payload: { id: "m_1", chatId: "1", userId: "9", role: "member" }
    });

    expect(state.chats).toHaveLength(1);
    expect(state.chats[0].id).toBe("1");
  });

  it("chat.deleted 携带成员载荷时应按事件 chatId 移除会话", () => {
    const state = createInitialState();
    state.chats = [{ id: "1", type: "private", title: "Alice" }];

    applyEventToState(state, {
      eventType: "chat.deleted",
      chatId: "1",
      payload: { id: "m_1", chatId: "1", userId: "9", role: "member" }
    });

    expect(state.chats).toHaveLength(0);
  });
});
