<script setup lang="ts">
import { computed, ref } from "vue";
import { FileUp, ListChecks, SendHorizonal, X } from "lucide-vue-next";
import type { Id, ImChat, ImMessageSend } from "../types/im";

const props = defineProps<{
  chat: ImChat | null;
  uploading: boolean;
}>();

const emit = defineEmits<{
  send: [message: ImMessageSend, file?: File];
}>();

const mode = ref<"text" | "file" | "poll">("text");
const content = ref("");
const pollOptions = ref("同意\n反对");
const file = ref<File | null>(null);

const canSend = computed(() => {
  if (!props.chat) {
    return false;
  }
  if (mode.value === "file") {
    return Boolean(file.value);
  }
  if (mode.value === "poll") {
    return content.value.trim() && pollOptions.value.split("\n").filter(Boolean).length >= 2;
  }
  return Boolean(content.value.trim());
});

function onFileChange(event: Event) {
  const target = event.target as HTMLInputElement;
  file.value = target.files?.[0] ?? null;
}

function submit() {
  if (!canSend.value) {
    return;
  }
  const clientMsgId = `c_${crypto.randomUUID()}`;
  const idempotentKey = `desktop_${clientMsgId}`;
  const common = { clientMsgId, idempotentKey };

  if (mode.value === "poll") {
    emit("send", {
      ...common,
      messageType: "poll",
      content: content.value.trim(),
      contentPayload: JSON.stringify({
        multiple: false,
        anonymous: false,
        options: pollOptions.value.split("\n").map((item) => item.trim()).filter(Boolean)
      })
    });
  } else if (mode.value === "file" && file.value) {
    emit("send", {
      ...common,
      messageType: file.value.type.startsWith("image/") ? "image" : "file",
      content: content.value.trim() || file.value.name
    }, file.value);
  } else {
    emit("send", {
      ...common,
      messageType: "text",
      content: content.value.trim()
    });
  }

  content.value = "";
  file.value = null;
  mode.value = "text";
}
</script>

<template>
  <footer class="composer">
    <div class="composer-modes">
      <button :class="{ active: mode === 'text' }" :disabled="!chat" @click="mode = 'text'">文本</button>
      <button :class="{ active: mode === 'file' }" :disabled="!chat" @click="mode = 'file'"><FileUp :size="16" /> 文件</button>
      <button :class="{ active: mode === 'poll' }" :disabled="!chat" @click="mode = 'poll'"><ListChecks :size="16" /> 投票</button>
    </div>

    <textarea
      v-model="content"
      :disabled="!chat"
      :placeholder="chat ? (mode === 'poll' ? '投票问题' : '输入消息') : '请选择会话'"
      @keydown.ctrl.enter.prevent="submit"
    ></textarea>

    <div v-if="mode === 'file'" class="composer-extra">
      <input type="file" :disabled="!chat || uploading" @change="onFileChange" />
      <button v-if="file" title="移除文件" @click="file = null"><X :size="16" /></button>
      <span>{{ file?.name || "选择要发送的文件" }}</span>
    </div>

    <div v-if="mode === 'poll'" class="composer-extra vertical">
      <textarea v-model="pollOptions" :disabled="!chat" placeholder="每行一个选项"></textarea>
    </div>

    <button class="send-button" :disabled="uploading || !canSend" @click="submit">
      <SendHorizonal :size="19" />
    </button>
  </footer>
</template>
