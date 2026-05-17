<script setup lang="ts">
import { computed, nextTick, ref, watch } from "vue";
import { CheckCheck, Download, MoreHorizontal, Pin, Smile, Trash2 } from "lucide-vue-next";
import type { Id, ImChat, ImMessage, ImPinnedMessage, ImPoll, ImUser } from "../types/im";
import { chatTitle, formatTime, idText, messagePreview, parsePayload, sameId } from "../lib/format";
import AvatarBadge from "./AvatarBadge.vue";

const props = defineProps<{
  chat: ImChat | null;
  messages: ImMessage[];
  pinned: ImPinnedMessage[];
  polls: Record<string, ImPoll>;
  profile: ImUser | null;
  loading: boolean;
}>();

const emit = defineEmits<{
  loadMore: [];
  reaction: [message: ImMessage, reaction: string];
  pin: [message: ImMessage];
  deleteMessage: [message: ImMessage];
  hideMessage: [message: ImMessage];
  read: [message: ImMessage];
  poll: [message: ImMessage, optionIds: Id[]];
}>();

const scroller = ref<HTMLElement | null>(null);

const title = computed(() => (props.chat ? chatTitle(props.chat, props.profile?.id) : "选择一个会话"));

watch(
  () => props.messages.length,
  async () => {
    await nextTick();
    scroller.value?.scrollTo({ top: scroller.value.scrollHeight });
  }
);

function isMine(message: ImMessage): boolean {
  return sameId(message.senderId, props.profile?.id);
}

function pollPayload(message: ImMessage): { options?: string[]; multiple?: boolean } | null {
  return parsePayload(message.contentPayload);
}
</script>

<template>
  <section class="thread-pane">
    <header class="thread-header">
      <div v-if="chat" class="thread-title">
        <AvatarBadge :src="chat.avatar" :label="title" />
        <div>
          <h2>{{ title }}</h2>
          <span>{{ chat.type }} · seq {{ chat.seq || 0 }}</span>
        </div>
      </div>
      <div v-else class="thread-title">
        <h2>AgentIM</h2>
        <span>P0 核心 IM 桌面端</span>
      </div>
    </header>

    <div v-if="chat && pinned.length" class="pinned-strip">
      <Pin :size="16" />
      <span v-for="item in pinned" :key="idText(item.id)">{{ messagePreview(item.message) }}</span>
    </div>

    <div ref="scroller" class="message-scroll">
      <button v-if="chat && messages.length" class="load-more" @click="emit('loadMore')">加载更早消息</button>
      <div v-if="!chat" class="empty-state">从左侧选择会话，或创建私聊、群组、频道。</div>
      <div v-else-if="loading" class="empty-state">消息加载中...</div>
      <div v-else-if="!messages.length" class="empty-state">暂无消息</div>

      <article
        v-for="message in messages"
        :key="idText(message.id)"
        class="message-item"
        :class="{ mine: isMine(message), deleted: message.status === 'deleted_all' }"
        @mouseenter="emit('read', message)"
      >
        <AvatarBadge v-if="!isMine(message)" size="sm" :label="message.senderNickname || message.senderUsername" />
        <div class="message-bubble">
          <div class="message-meta">
            <span>{{ message.senderNickname || message.senderUsername || message.senderId }}</span>
            <time>{{ formatTime(message.createTime) }}</time>
            <small v-if="message.status === 'edited'">已编辑</small>
          </div>

          <div v-if="message.replyToMessageId" class="reply-ref">回复 #{{ message.replyToMessageId }}</div>
          <div v-if="message.forwardFromMessageId" class="reply-ref">转发自消息 #{{ message.forwardFromMessageId }}</div>

          <template v-if="message.messageType === 'poll'">
            <h3>{{ message.content }}</h3>
            <div class="poll-options">
              <button
                v-for="option in polls[idText(message.id)]?.options"
                :key="idText(option.id)"
                :class="{ selected: option.selectedByMe }"
                @click="emit('poll', message, [option.id])"
              >
                <span>{{ option.text }}</span>
                <strong>{{ option.voteCount || 0 }}</strong>
              </button>
              <button
                v-for="(option, index) in !polls[idText(message.id)] ? pollPayload(message)?.options || [] : []"
                :key="option"
                @click="emit('poll', message, [index])"
              >
                <span>{{ option }}</span>
              </button>
            </div>
          </template>
          <template v-else>
            <p>{{ messagePreview(message) }}</p>
          </template>

          <div v-if="message.resourceIds" class="resource-line">
            <Download :size="14" />
            <span>资源 {{ message.resourceIds }}</span>
          </div>

          <div v-if="message.reactions?.length" class="reaction-row">
            <span v-for="item in message.reactions" :key="item.reaction">{{ item.reaction }} {{ item.count }}</span>
          </div>

          <div class="message-actions">
            <button title="反应" @click="emit('reaction', message, '+1')"><Smile :size="15" /></button>
            <button title="置顶" @click="emit('pin', message)"><Pin :size="15" /></button>
            <button title="标记已读" @click="emit('read', message)"><CheckCheck :size="15" /></button>
            <button title="仅自己隐藏" @click="emit('hideMessage', message)"><MoreHorizontal :size="15" /></button>
            <button title="撤回" @click="emit('deleteMessage', message)"><Trash2 :size="15" /></button>
          </div>
        </div>
      </article>
    </div>
  </section>
</template>
