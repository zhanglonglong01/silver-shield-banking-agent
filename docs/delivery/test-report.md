# M8 测试与验收报告

报告日期：2026-09-11。以下结果均来自本机最终工作树的实际命令输出；默认部署使用 `offline-demo`，不连接真实银行数据或外部模型。

## 质量门禁

| 门禁 | 命令 | 结果 |
|---|---|---|
| 后端单元/集成/ArchUnit/Testcontainers | Java 17 Maven 容器内执行 `mvn test` | **PASS**：24 suites，162 tests，0 failures/errors/skipped；MySQL 8.4 与 Redis 7.4 Testcontainers 实际运行 |
| 前端类型检查与生产构建 | `npm run build` | **PASS**：Vue TypeScript 检查通过，Vite 处理 1610 modules |
| 前端 Vitest | `npm test` | **PASS**：4 files，12/12 tests |
| Playwright 六类业务 | `npm run test:e2e` | **PASS**：Chromium 9/9；同一套用例对 Docker Web `127.0.0.1:3000` 再次 9/9 |
| 依赖漏洞 | `npm audit --registry=https://registry.npmjs.org --audit-level=high` | **PASS**：0 vulnerabilities |
| Compose 构建与启动 | 独立 project name + 空 volumes + 备用端口 | **PASS**：从零创建 API/MySQL/Redis/Web，四服务 healthy；9/9 验收后删除隔离测试栈。默认栈四个主机端口均为 loopback，API 以 UID 10001 运行 |
| Flyway 与数据库 | Compose MySQL 查询 | **PASS**：5/5 migrations，schema version `v005`，22 tables（含 Flyway history） |
| 一键 API/Agent 验收 | `scripts/acceptance.ps1` | **PASS**：9/9；固定摘要、AAL1、越权、只读/跨场景、PREPARE 零副作用、注入、注销和 OpenAPI 均通过 |
| 只读并发烟测 | `scripts/load-smoke.ps1 -Requests 100 -Concurrency 10` | **PASS**：100/100，0 failures；批次客户端延迟 min 9 ms、p95 23 ms、max 23 ms |
| 依赖故障注入 | `scripts/fault-smoke.ps1` | **PASS**：Redis 停止时 503、恢复后 200；MySQL 停止时连接失败、恢复后 200 |
| 脚本与路径安全 | PowerShell 解析 + `scripts/tests/test-project-path-safety.ps1` | **PASS**：8 scripts 解析错误 0；路径逃逸/重解析点测试 9/9 |
| 备份与契约导出 | `backup-sandbox.ps1`、`export-openapi.ps1` | **PASS**：生成 126233-byte SQL 快照与 16254-byte OpenAPI 3.1 JSON |

## 固定验收事实

- Dataset version：`fintechathon-demo-v2`；seed：`2026`；baseline：`2026-08-31`。
- 连续 reset 后规范化摘要均为 `6c9b472735d704368ae57afe1abb536563b6bd6221404fad0c4f0c986fab65b3`。
- Alice 可见 2 个账户、2 张卡、6 个订阅和 8 个理财产品；跨用户资源返回 404，匿名和已注销 token 返回 401。
- 转账、卡片、订阅和理财对话只创建待确认操作；验收前后账户、卡片和订阅快照一致。理财购买计划要求 AAL2。
- Prompt Injection 验收调用 Tool 数为 0；畸形/超长 Tool ID、越权 ID、篡改/过期快照与幻觉 Tool 均由自动化攻击测试覆盖。
- Web、Swagger UI 和经 Nginx 代理的 OpenAPI 均返回 200；API 日志敏感标记扫描结果为 0。

## 已覆盖安全性质

- 正常、异常、越权、Prompt Injection、幻觉 Tool/ID/数字/成功声明。
- 动态 Allowed Tool Set、执行端二次成员验证、精确 Registry、严格参数 schema。
- 写操作 PREPARE 零业务副作用、AAL2、独立确认、过期、重放、TOCTOU、幂等和并发双确认。
- `prod` 及 `prod,offline-demo` 混合 profile 均不注册 reset、禁止固定码 AAL2，并将遗留演示 AAL2 会话降级；login→access→logout→同 token 401。
- Agent/Tool→Repository 和 Agent→Handler/Registry 的 ArchUnit 边界。
- 固定 dataset version/seed/baseline/digest 的连续 reset 一致性。

## 范围说明

并发烟测使用复用 `HttpClient`、按 10 请求批次发起，结果用于单机比赛部署的基本稳定性检查，不是容量结论。OWASP 文档是控制映射，不是渗透测试认证。默认测试不连接真实 LLM；真实 Provider 只在显式 profile 与外部密钥存在时运行。

Flyway 启动日志提示其正式测试范围截至 MySQL 8.1，而本项目使用 MySQL 8.4；本轮 5 个 migration、Compose 和 162 个后端测试均通过，仍将其保留为升级前的兼容性风险。离线 Provider 的注入关键词仅是增强层，最终安全边界始终是 Capability Policy、签名 Allowed Tool Set 与 SecureToolGateway。

本机当前未安装可被 `JAVA_HOME` 发现的 JDK，因此最终后端测试通过 Eclipse Temurin 17 Maven 容器并挂载 Docker socket 执行；这验证了 Java 17 编译和真实 Testcontainers，但若要直接在 Windows 主机运行 Maven Wrapper，仍需另行安装 JDK 17。
