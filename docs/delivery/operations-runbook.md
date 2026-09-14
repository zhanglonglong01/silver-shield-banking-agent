# 部署与运行手册

## 冷启动

要求 Docker Desktop/Engine、Docker Compose 和 PowerShell 7（`pwsh`）。仓库根目录执行：

```powershell
Copy-Item .env.example .env
docker compose up --build -d --wait
docker compose ps
Invoke-RestMethod http://localhost:8080/actuator/health
```

默认 `SPRING_PROFILES_ACTIVE=offline-demo`，无需公网或 API Key。Web 为 `http://localhost:3000`，API 为 `http://localhost:8080`，Swagger UI 为 `http://localhost:8080/swagger-ui.html`。

Compose 默认将 MySQL、Redis、API 和 Web 的主机端口仅绑定到 `127.0.0.1`。如确需在受信任局域网展示，须在 `.env` 中显式把 `API_BIND_ADDRESS`、`WEB_BIND_ADDRESS` 设置为指定的本机局域网地址，并同时更换演示凭据、限制防火墙来源；不要将带固定 sandbox 凭据和 reset endpoint 的演示栈直接暴露到公网。

## 一键验收

```powershell
.\scripts\acceptance.ps1
.\scripts\load-smoke.ps1 -Requests 100 -Concurrency 10
.\scripts\fault-smoke.ps1
```

验收脚本验证健康、固定 reset、AAL1、本人与越权资源、离线账户/账单/转账准备、注入零 Tool、logout revoke 和 OpenAPI；不确认任何资金写操作。故障脚本会依次短暂停止 Redis、MySQL 并验证健康状态 fail closed，随后恢复，勿在共享或生产环境执行。

## 数据 reset 与备份

```powershell
.\scripts\backup-sandbox.ps1
.\scripts\reset-sandbox.ps1
```

备份输出在被 Git 忽略的 `artifacts/backups/`。reset 仅在 `dev`、`test`、`offline-demo` 注册，并要求 `DEMO_ADMIN`；`prod` profile 无 controller、initializer 和 reset service。恢复演示基线首选 reset，因为数据集版本、seed、日期和摘要固定。SQL 备份用于排障快照，不作为生产备份方案。

## 使用真实兼容模型（可选）

将未提交的 `.env` 中 profile 改为 `dev`，并设置 `SPRING_AI_CHAT_MODEL=openai`、`OPENAI_API_KEY`、`OPENAI_BASE_URL`、`OPENAI_MODEL`。不要在仓库、命令历史、日志或截图中暴露真实密钥。真实 Provider 不改变 Gateway 的授权边界。

## 健康与日志

- `docker compose ps`：四个服务应为 healthy。
- `docker compose logs api --tail 100`：确认 Flyway migration 和请求状态。
- `GET /actuator/health`：只返回聚合 UP/DOWN，不公开数据库细节。
- `X-Trace-Id`：用于关联一次请求；日志不得包含 Authorization 或 body。

## 故障处理

| 现象 | 检查 | 安全行为 |
|---|---|---|
| API unhealthy | MySQL/Redis health、端口占用、API 日志 | 不绕过依赖启动 |
| `PROVIDER_UNAVAILABLE` | 是否误用 `dev` + model `none` | 切回 `offline-demo` 或正确配置 Provider |
| Redis 不可用 | `docker compose ps redis` | 登录/会话失败；不会降级为匿名授权 |
| DB 不可用 | MySQL health、Flyway 日志 | Tool/写操作失败；不会返回虚假成功 |
| reset 404 | 当前 profile 是否 prod | prod 按设计禁止 reset |
| 端口冲突 | 设置 `MYSQL_PORT/REDIS_PORT/API_PORT/WEB_PORT` | 容器内部服务名不变；绑定地址由 `API_BIND_ADDRESS/WEB_BIND_ADDRESS` 单独控制 |

停止服务使用 `docker compose down`。不要在含需保留数据的环境执行 `down -v`；比赛沙箱虽然可重建，删除 volume 仍是破坏性操作。
