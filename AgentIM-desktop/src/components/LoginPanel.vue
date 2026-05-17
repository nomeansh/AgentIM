<script setup lang="ts">
import { computed, reactive, ref } from "vue";
import { MessageCircle, Server, ShieldCheck } from "lucide-vue-next";
import type { ConnectionSettings, LoginRequest, RegisterRequest } from "../types/im";

const props = defineProps<{
  settings: ConnectionSettings;
  loading: boolean;
  error: string;
}>();

const emit = defineEmits<{
  login: [body: LoginRequest, settings: ConnectionSettings];
  register: [body: RegisterRequest, settings: ConnectionSettings];
  updateSettings: [settings: ConnectionSettings];
}>();

const mode = ref<"login" | "register">("login");
const form = reactive({
  username: "",
  password: "",
  confirmPassword: ""
});
const localSettings = reactive<ConnectionSettings>({ ...props.settings });

const canSubmit = computed(() => {
  if (!form.username.trim() || !form.password.trim()) {
    return false;
  }
  return mode.value === "login" || form.password === form.confirmPassword;
});

function buildBody(): LoginRequest {
  return {
    clientId: localSettings.clientId,
    grantType: "password",
    tenantId: localSettings.tenantId,
    username: form.username.trim(),
    password: form.password
  };
}

function submit() {
  emit("updateSettings", { ...localSettings });
  if (mode.value === "login") {
    emit("login", buildBody(), { ...localSettings });
  } else {
    emit("register", { ...buildBody(), userType: "app_user" }, { ...localSettings });
  }
}
</script>

<template>
  <main class="login-shell">
    <section class="login-product">
      <div class="brand-mark"><MessageCircle :size="30" /></div>
      <h1>AgentIM</h1>
      <p>面向私有部署的云消息桌面端</p>
      <div class="login-facts">
        <div><ShieldCheck :size="18" /> Sa-Token 会话</div>
        <div><Server :size="18" /> Tauri Windows 桌面端</div>
      </div>
    </section>

    <section class="login-panel">
      <div class="segmented">
        <button :class="{ active: mode === 'login' }" @click="mode = 'login'">登录</button>
        <button :class="{ active: mode === 'register' }" @click="mode = 'register'">注册</button>
      </div>

      <label>
        <span>用户名</span>
        <input v-model="form.username" autocomplete="username" placeholder="例如 alice" />
      </label>
      <label>
        <span>密码</span>
        <input v-model="form.password" autocomplete="current-password" type="password" placeholder="至少 5 位" />
      </label>
      <label v-if="mode === 'register'">
        <span>确认密码</span>
        <input v-model="form.confirmPassword" autocomplete="new-password" type="password" />
      </label>

      <div class="settings-grid">
        <label>
          <span>认证地址</span>
          <input v-model="localSettings.authBaseUrl" placeholder="http://localhost:9210" />
        </label>
        <label>
          <span>业务地址</span>
          <input v-model="localSettings.apiBaseUrl" placeholder="http://localhost:9211" />
        </label>
        <label>
          <span>WebSocket 路径</span>
          <input v-model="localSettings.wsPath" placeholder="/ws/im" />
        </label>
        <label>
          <span>clientid</span>
          <input v-model="localSettings.clientId" placeholder="agentim-web" />
        </label>
        <label>
          <span>租户上下文</span>
          <input v-model="localSettings.tenantId" placeholder="000000" />
        </label>
      </div>

      <p v-if="mode === 'register' && form.confirmPassword && form.password !== form.confirmPassword" class="form-error">
        两次输入的密码不一致
      </p>
      <p v-if="error" class="form-error">{{ error }}</p>

      <button class="primary-action" :disabled="loading || !canSubmit" @click="submit">
        {{ loading ? "处理中..." : mode === "login" ? "进入桌面端" : "创建账号并登录" }}
      </button>
    </section>
  </main>
</template>
