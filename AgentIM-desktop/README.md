# AgentIM Desktop

P0 核心 IM 的 Windows 桌面端，基于 Vue 3、TypeScript、Vite 和 Tauri v2。

## 本地运行

```powershell
npm install
npm run dev
npm run tauri dev
```

默认连接 `http://localhost:9211`，WebSocket 默认路径为 `/ws/im`。如果后端经 Gateway 暴露，请在登录页调整后端地址；桌面端会同时发送 `Authorization: Bearer <access_token>` 和 `clientid` 请求头。

## 验证

```powershell
npm test
npm run build
npm run tauri build
```

`npm run tauri build` 需要 Windows 已安装 Microsoft C++ Build Tools、Rust MSVC 工具链和 WebView2。
