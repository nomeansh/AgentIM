<script setup lang="ts">
import { reactive, watch } from "vue";
import { RefreshCw, Save } from "lucide-vue-next";
import type { ConnectionSettings, ImUserProfileUpdate, ImUser } from "../types/im";

const props = defineProps<{
  settings: ConnectionSettings;
  profile: ImUser | null;
  loading: boolean;
}>();

const emit = defineEmits<{
  saveSettings: [settings: ConnectionSettings];
  saveProfile: [profile: ImUserProfileUpdate];
  reload: [];
}>();

const localSettings = reactive<ConnectionSettings>({ ...props.settings });
const profileForm = reactive<ImUserProfileUpdate>({});

watch(
  () => props.settings,
  (value) => Object.assign(localSettings, value),
  { deep: true }
);

watch(
  () => props.profile,
  (value) => {
    Object.assign(profileForm, {
      nickname: value?.nickname ?? "",
      bio: value?.bio ?? "",
      avatar: value?.avatar ?? "",
      phone: value?.phone ?? "",
      email: value?.email ?? ""
    });
  },
  { immediate: true }
);
</script>

<template>
  <section class="side-panel">
    <header class="pane-header">
      <div>
        <h2>设置</h2>
        <span>连接与资料</span>
      </div>
      <button title="重新加载数据" @click="emit('reload')"><RefreshCw :size="17" /></button>
    </header>

    <div class="form-card">
      <h3>连接</h3>
      <label><span>认证地址</span><input v-model="localSettings.authBaseUrl" /></label>
      <label><span>业务地址</span><input v-model="localSettings.apiBaseUrl" /></label>
      <label><span>WebSocket 路径</span><input v-model="localSettings.wsPath" /></label>
      <label><span>clientid</span><input v-model="localSettings.clientId" /></label>
      <label><span>租户上下文</span><input v-model="localSettings.tenantId" /></label>
      <button class="primary-action compact" :disabled="loading" @click="emit('saveSettings', { ...localSettings })">
        <Save :size="17" /> 保存连接设置
      </button>
    </div>

    <div class="form-card">
      <h3>个人资料</h3>
      <label><span>昵称</span><input v-model="profileForm.nickname" /></label>
      <label><span>头像 URL / resourceId</span><input v-model="profileForm.avatar" /></label>
      <label><span>简介</span><textarea v-model="profileForm.bio"></textarea></label>
      <label><span>电话</span><input v-model="profileForm.phone" /></label>
      <label><span>邮箱</span><input v-model="profileForm.email" /></label>
      <button class="primary-action compact" :disabled="loading || !profile" @click="emit('saveProfile', { ...profileForm })">
        <Save :size="17" /> 保存资料
      </button>
    </div>
  </section>
</template>
