# 银盾智行 Web

M7 建立、M8 完成交付加固的演示型前端，采用 Vue 3、TypeScript、Vite、Pinia 与 Vue Router。前端是交互与展示层，不是银行操作的安全边界。

## 本地开发

先在仓库根目录启动 MySQL、Redis 和 API：

```bash
docker compose up -d mysql redis api
```

再启动前端：

```bash
cd frontend
npm ci
npm run dev
```

访问 `http://localhost:5173`。Vite 将 `/api` 同源代理至 `http://127.0.0.1:8080`。

## 质量门禁

```bash
npm run typecheck
npm run test
npm run test:e2e
npm run build
```

Playwright 使用网络层固定响应验证六类前端演示路径，避免依赖公网模型或写入沙箱。M8 根目录验收脚本额外覆盖 Compose 中真实 API 的登录、本人资源、越权隔离、离线 Agent、Sandbox reset、注销撤销和 OpenAPI；浏览器确认分支在比赛环境变更后仍应人工彩排。

## 安全边界

- Bearer token 仅保存在内存与当前标签页的 `sessionStorage`，不写入 URL、日志或长期存储。
- 所有模型文本使用 Vue 文本插值渲染，不使用 `v-html`。
- 写请求只能显示后端签发的 `pendingActions`；确认调用独立 `/operations/{id}/confirm`，body 只包含 `challengeId` 与 `confirmed`。
- AAL2 在提交前显式升级；前端按钮状态不是最终授权，服务端仍会复核。
- WebMCP 只注册 `start_banking_agent_request`，最多发起查询或准备操作，不注册确认/提交工具；输入拒绝多余字段。
- Nginx 配置 CSP、`nosniff`、拒绝 framing、禁用敏感 API 缓存，并同源反向代理后端。

Compose 默认激活 `offline-demo`，并保持 `SPRING_AI_CHAT_MODEL=none`；固定的 `OfflineDemoBankingModelClient` 不访问公网，但仍只提议本轮 Allowed Tool Set 中的调用，所有调用照常经过 `SecureToolGateway`。若直接使用 `dev` 且没有启用兼容模型，页面会如实展示 `PROVIDER_UNAVAILABLE`，不会伪造成功。
