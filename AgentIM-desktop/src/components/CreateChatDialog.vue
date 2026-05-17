<script setup lang="ts">
import { computed, reactive } from "vue";
import type { ImChatCreate } from "../types/im";

defineProps<{
  open: boolean;
}>();

const emit = defineEmits<{
  close: [];
  create: [body: ImChatCreate];
}>();

const form = reactive({
  type: "group" as "private" | "group" | "channel",
  title: "",
  description: "",
  memberIdsText: ""
});

const body = computed<ImChatCreate>(() => ({
  type: form.type,
  title: form.title.trim() || undefined,
  description: form.description.trim() || undefined,
  memberIds: form.memberIdsText
    .split(",")
    .map((item) => item.trim())
    .filter(Boolean)
}));

function submit() {
  emit("create", body.value);
}
</script>

<template>
  <div v-if="open" class="dialog-backdrop" @click.self="emit('close')">
    <section class="dialog">
      <header>
        <h2>新建会话</h2>
        <button @click="emit('close')">关闭</button>
      </header>
      <label>
        <span>类型</span>
        <select v-model="form.type">
          <option value="private">私聊</option>
          <option value="group">群组</option>
          <option value="channel">频道</option>
        </select>
      </label>
      <label v-if="form.type !== 'private'">
        <span>标题</span>
        <input v-model="form.title" placeholder="会话标题" />
      </label>
      <label>
        <span>{{ form.type === 'private' ? '对方用户 ID' : '初始成员 ID，逗号分隔' }}</span>
        <input v-model="form.memberIdsText" placeholder="1002,1003" />
      </label>
      <label v-if="form.type !== 'private'">
        <span>简介</span>
        <textarea v-model="form.description"></textarea>
      </label>
      <button class="primary-action compact" @click="submit">创建</button>
    </section>
  </div>
</template>
