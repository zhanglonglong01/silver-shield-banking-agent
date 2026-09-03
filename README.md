# 银盾智行

> 面向个人银行业务的安全可信智能体（AI Banking Agent）

本项目面向 2026 FinTechathon 深圳国际金融科技大赛国内赛道。目标是在完全模拟的银行沙箱中，让用户通过自然语言完成银行任务，同时对每一次允许、待确认和拒绝给出可验证、可审计的依据。

当前状态：**M6——多轮安全任务 DAG 与跨场景联动已实现**。六类核心业务均已有后端闭环；跨场景计划最多 8 步，每一步只暴露依赖已满足的 Allowed Tool Set，写步骤最多一个且只能创建待确认操作。Multi-Agent、RAG 和前端仍未实现。

## M1–M6 快速启动

前置条件：Docker Compose。所有账号、账户、卡片、流水、收款人和产品均为虚构数据。

```bash
docker compose up --build
```

服务地址：`http://localhost:8080`，健康检查：`GET /actuator/health`。

演示凭据：

| 用户名 | 密码 | 用途 |
|---|---|---|
| `demo.alice` | `Demo@2026!` | 主演示客户，2 个账户 |
| `demo.bob` | `Demo@2026!` | 越权隔离对照客户，1 个账户 |
| `demo.admin` | `Admin@2026!` | 仅 dev/test 可重置 Sandbox |
| `demo.locked` | `Demo@2026!` | 锁定账号拒绝场景 |

登录：

```http
POST /api/v1/auth/login
Content-Type: application/json

{"username":"demo.alice","password":"Demo@2026!"}
```

响应中的 bearer token 只存于 Redis，服务端保存 token 的 SHA-256 key，不保存明文 token。后续请求使用 `Authorization: Bearer <token>`。

### M1 API

| 方法 | 路径 | 说明 |
|---|---|---|
| `POST` | `/api/v1/auth/login` | 用户名密码登录，签发 AAL1 会话 |
| `POST` | `/api/v1/auth/logout` | 注销当前 token |
| `GET` | `/api/v1/auth/me` | 当前用户与 AAL |
| `GET` | `/api/v1/auth/assurance` | 查询当前认证强度与 AAL2 升级可用性 |
| `POST` | `/api/v1/auth/step-up` | dev/test 模拟 AAL2 升级，默认演示码 `246810`，有效 5 分钟 |
| `GET` | `/api/v1/accounts` | 本人账户列表 |
| `GET` | `/api/v1/accounts/{id}` | 本人账户；他人资源统一返回 404 |
| `GET` | `/api/v1/accounts/{id}/transactions?from=2026-03-01&to=2026-08-31&limit=50` | 本人流水 |
| `GET` | `/api/v1/cards` | 本人卡片 |
| `GET` | `/api/v1/payees` | 本人收款人 |
| `GET` | `/api/v1/subscriptions` | 本人订阅 |
| `GET` | `/api/v1/wealth-products` | 模拟理财产品 |
| `GET` | `/api/v1/sandbox/metadata` | dev/test 数据集版本、seed、摘要和行数 |
| `POST` | `/api/v1/sandbox/reset` | 仅 dev/test + DEMO_ADMIN；确定性重置 |
| `POST` | `/api/v1/tools/allowed` | 按当前 Principal、AAL 和候选集合生成短期 Allowed Tool Set |
| `POST` | `/api/v1/tools/execute` | 通过 SecureToolGateway 执行只读或“仅准备写入”Tool；所有准备操作建议携带 `Idempotency-Key` |
| `POST` | `/api/v1/agent/chat` | 银行 Agent；写请求只返回 `pendingActions`，不会在对话中产生业务副作用 |
| `GET` | `/api/v1/operations/{operationId}` | 查询本人操作单状态；他人操作统一 404 |
| `POST` | `/api/v1/operations/{operationId}/confirm` | 独立确认，只接受 `challengeId` 与 `confirmed`，不接受金额等业务参数 |

`prod` profile 不装配 Sandbox initializer、reset service 或 reset controller。
`prod` 默认不配置模拟 AAL2 验证码，因此 step-up fail closed；演示码只用于 dev/test 虚构沙箱。

dev/test 一键 reset：

```powershell
.\scripts\reset-sandbox.ps1
```

M5 固定数据集为 `fintechathon-demo-v2`，seed 为 `2026`，baseline date 为 `2026-08-31`；当前规范化持久化摘要为 `6c9b472735d704368ae57afe1abb536563b6bd6221404fad0c4f0c986fab65b3`。该摘要覆盖固定风险画像和基础银行资源，不包含运行时操作单；连续 reset 后摘要和本人资源快照必须完全一致。

Agent 请求示例：

```http
POST /api/v1/agent/chat
Authorization: Bearer <token>
Content-Type: application/json

{"message":"我这个月花了多少钱？","conversationId":null}
```

### M4 转账演示

1. 先用 `GET /api/v1/accounts` 与 `GET /api/v1/payees` 取得当前用户可见的 `sourceAccountId` 和 `payeeId`。
2. 通过 Gateway 准备转账（此时余额不变）：

```http
POST /api/v1/tools/execute
Authorization: Bearer <token>
Idempotency-Key: demo-transfer-0001
Content-Type: application/json

{
  "toolName":"prepare_transfer",
  "toolVersion":"1.0",
  "arguments":{
    "sourceAccountId":"<owned-account-id>",
    "payeeId":"<owned-payee-id>",
    "amount":"88.00",
    "currency":"CNY",
    "purpose":"比赛演示"
  }
}
```

3. 若返回 `requiredAal=AAL2`，调用 `POST /api/v1/auth/step-up`，body 为 `{"code":"246810"}`；再使用同一 bearer token 发起下一步。
4. 从准备结果取得 `operationId` 与 `challengeId`，显式确认：

```http
POST /api/v1/operations/<operationId>/confirm
Authorization: Bearer <token>
Content-Type: application/json

{"challengeId":"<challenge-id>","confirmed":true}
```

确认接口不接受金额、收款人或来源账户；重复确认返回原始回执，不会重复扣款。聊天中输入“确认”也不会调用最终执行器。

### M5 卡片、订阅与理财演示

M5 共增加 8 个显式 Tool：`get_card_controls`、`prepare_card_status_change`、`prepare_card_controls_change`、`analyze_subscription_costs`、`prepare_subscription_cancellation`、`check_wealth_suitability`、`get_wealth_portfolio`、`prepare_wealth_purchase`。其中 4 个 `prepare_*` 只创建待确认操作；最终执行器不注册为 Tool。

演示顺序：先通过已有 REST 查询取得本人资源 ID，再调用 `/api/v1/tools/execute`。卡片解冻、放宽控制和所有理财购买要求 AAL2；冻结、收紧控制和取消订阅为 AAL1，但仍要求独立确认。理财必须先获得与用户、产品、付款账户、金额和币种精确绑定且未过期的适当性检查 ID。所有确认仍使用上方相同的 `/api/v1/operations/{operationId}/confirm` 契约。

订阅月度等值成本由 Java 确定性计算：周付按 `金额 × 52 ÷ 12`、季付按 `金额 ÷ 3`、年付按 `金额 ÷ 12`；LLM 不参与金额运算。适当性检查虽归类为只读业务能力，但会持久化短期、不可复用换参的决策证据，不会扣款或建立持仓。

默认 `SPRING_AI_CHAT_MODEL=none`，无 API Key 也能安全启动。需要使用 OpenAI-compatible Provider 时，在本机环境变量或未提交的 `.env` 中配置 `SPRING_AI_CHAT_MODEL=openai`、`OPENAI_API_KEY`、`OPENAI_BASE_URL` 和 `OPENAI_MODEL`；禁止把真实密钥写入仓库。

### M6 跨场景编排演示

M6 不增加万能 Tool，也不增加最终写执行器。`POST /api/v1/agent/chat` 的响应新增 `taskPlan`，展示 `planId`、计划状态、最多 8 个拓扑有序步骤、依赖、步骤状态、Evidence ID 和稳定原因码。

两条可重复黄金路径：

```json
{"message":"分析本月账单，然后取消视频订阅"}
```

按 `get_accounts/list_transactions + list_subscriptions/analyze_subscription_costs → prepare_subscription_cancellation` 执行，最终停在 `AWAITING_CONFIRMATION`，订阅状态不变。

```json
{"message":"查看账户余额并购买适合我的理财产品"}
```

按 `get_accounts/get_account_balance + list_wealth_products/get_wealth_portfolio → check_wealth_suitability → prepare_wealth_purchase` 执行，最终停在 `AWAITING_CONFIRMATION`，余额与持仓不变。

若目标不完整，返回 `NEEDS_INPUT`；在同一 `conversationId` 下补充场景即可继续建图。Provider/Tool 故障返回暂停状态，发送“继续执行”会从已审计 Evidence 恢复；发送“取消任务”只取消编排，不调用任何银行写操作。模型跳过依赖、添加隐藏 Tool、声明错误执行模式、构造超过 8 步或包含多个写步骤的计划均 fail closed。

### 本地测试

项目带 Maven Wrapper：

```bash
./mvnw test
```

存在 Docker 时，Testcontainers 会以 MySQL 8.4 和 Redis 7.4 运行登录、Flyway、本人查询、越权和 reset 集成测试；Docker 不可用时这些测试明确标记为 skipped，单元与 ArchUnit 测试仍执行。

## 设计原则

1. **LLM 只提议，不裁决、不落库**：模型负责理解、规划和生成受约束的 Tool Call；确定性安全内核负责参数、权限、风险、确认、幂等和执行。
2. **默认拒绝**：未注册 Tool、未知 ID、缺失证据、越权资源、失效确认和不完整参数一律不执行。
3. **查询与变更分离**：查询 Tool 返回可引用证据；所有资金、卡片状态/控制、订阅取消和理财购买都生成不可变操作单并要求明确确认。
4. **用户身份不来自模型参数**：`userId`、角色和认证等级只从服务端安全上下文取得。
5. **解释与审计同源**：页面展示的“为什么允许/拒绝”来自参数、权限、风险和确认阶段产生的决策凭证，不由 LLM 编造。
6. **Mock First**：只使用版本化、可重置、可复现的模拟银行数据；不接触真实银行账号或生产接口。
7. **工具可见性不是授权**：每轮请求由 Capability/Tool Policy 动态计算 Allowed Tool Set；模型侧只看见该集合，执行端仍按同一策略快照再次校验 `toolName`。

## 六类核心场景

| 场景 | 最小可演示闭环 | 安全要求 |
|---|---|---|
| 智能转账 | 理解收款人/金额 → 预览 → 风险判断 → 确认 → 转账 → 回执 | 余额、限额、收款人归属、频率、按风险升级 AAL2、幂等 |
| 账单分析 | 查询流水 → 确定性聚合 → 趋势/分类/异常说明 | 仅本人数据、数字可追溯到证据 |
| 理财操作 | 产品筛选 → 适当性检查 → 购买预览 → 确认 → 下单 | 风险等级、产品状态、余额、AAL2、幂等 |
| 卡片管理 | 查询卡片 → 冻结/解冻/调整控制 → 确认 → 状态回执 | 所有权、合法状态机、按动作升级 AAL2、幂等 |
| 订阅/代扣 | 识别周期扣款 → 查询订阅 → 取消预览 → 确认 → 取消 | 所有权、当前状态、影响说明、幂等 |
| 跨场景联动 | 生成多步骤计划并串联上述原子 Tool | DAG 校验、逐步证据、写步骤逐项或批量绑定确认 |

## 目标架构

采用前后端分离的模块化单体：Vue 3 前端通过 REST API 访问一个 Spring Boot 3 应用；MySQL 8 保存业务事实、操作单和审计证据；Redis 只保存可丢失的会话加速数据、限流计数和短期锁。Agent 通过 Spring AI 调用显式白名单 Tool，Tool 只能进入安全执行网关，不能直接调用 Repository。

写操作固定经过：

```text
Agent → Tool → 参数校验 → 权限检查 → 风险检查 → 用户确认
      → 最终安全复核 → Service → Database → Audit Log
```

详细设计：

- [系统架构](docs/architecture.md)
- [数据库 ER 模型](docs/database-er.md)
- [Tool 定义](docs/tool-catalog.md)
- [安全执行模型](docs/security-execution-model.md)
- [项目目录与模块边界](docs/project-structure.md)
- [测试策略与安全场景矩阵](docs/testing-strategy.md)

## 技术基线

- Java 17、Spring Boot 3、Spring AI
- MySQL 8、Redis、Flyway
- Vue 3、TypeScript、Vite、Pinia
- REST API、OpenAPI 3
- JUnit 5、AssertJ、Mockito、Testcontainers、ArchUnit、Vitest、Playwright
- Docker Compose

模型通过项目自有 `BankingModelClient` 端口解耦，Spring AI 1.1.8 的 `ChatModel` 只是 OpenAI-compatible 适配实现。自动化测试使用确定性的 Fake Model，不依赖公网或付费模型；真实模型测试只在显式 `real-llm` profile 且存在环境变量密钥时运行。

## 开发 Roadmap

### M0：设计冻结

- [x] 系统架构与信任边界
- [x] ER 模型与 Mock 数据方案
- [x] Tool 名称、参数、返回信封与版本策略
- [x] 安全执行管线、风险规则、确认和审计模型
- [x] 目录结构、测试金字塔和开发里程碑
- [x] 用户确认设计决策与首个实现模块

退出条件：核心写链路、数据所有权、确认绑定、拒绝解释和测试要求没有歧义。

### M1：工程骨架与可运行沙箱

- [x] 创建 Java 17、Spring Boot 3、Maven、Docker Compose、环境配置和健康检查。
- [x] Flyway 建表，加入固定版本的演示数据生成器与一键重置。
- [x] 建立 Redis bearer 会话、Spring Security、AAL1/AAL2 模型和统一安全错误。
- [x] 建立 User、Account、Card、Transaction、Payee、Subscription、WealthProduct 基础领域与本人资源查询。
- [x] 配置 MySQL/Redis Testcontainers、JUnit 5 和 ArchUnit。
- [x] **M1 不引入 LLM、Agent Planner、Tool Calling、RAG 或完整 Agent Tool 实现。**

退出条件：`docker compose up` 可启动；演示用户可登录；迁移与 Mock 数据校验测试通过。

### M2：Secure Tool Foundation

- [x] 实现 Capability Policy、HMAC 完整性保护的 `AllowedToolSetSnapshot` 和 60 秒有效期。
- [x] 实现 7 项显式注册的只读 Tool；Registry 不做名称/版本 fallback。
- [x] 实现 `SecureToolGateway`：执行端成员复核 → Registry → 定义摘要 → 严格参数校验 → Handler → 审计。
- [x] Tool Handler 只依赖 `BankingReadUseCase`，不依赖 Repository；Agent 禁止依赖 Handler/Registry。
- [x] 增加 allow/deny、越权、injection-like malicious input / Tool argument boundary、幻觉 ID、快照过期/篡改/错绑与审计失败测试。M2 尚无 LLM，因此不把该项称为真正的 Prompt Injection 测试。
- [x] 增加 `tool_security_audit` 追加记录；只保存参数 SHA-256，不保存 Bearer、Prompt 或原始参数。
- [x] 保持 Spring AI、LLM、Planner、RAG、写操作、AAL2 流程全部未接入。

退出条件：动态允许集合和 7 个只读 Tool 可部署演示；任何 Tool 执行只能通过 Gateway；完整测试通过。

### M3：账户与账单分析

- [x] 接入 Spring AI 1.1.8/OpenAI-compatible Provider，同时默认关闭 Provider、fallback 和自动 Tool Calling。
- [x] 实现最多 5 轮的应用自控 Agent Loop、Provider/Tool 超时、重复调用、调用数量、上下文和异常响应防护。
- [x] 从本轮 `AllowedToolSetSnapshot` 动态生成 7 个 Tool 的 JSON Schema；未允许 Tool 不暴露。
- [x] 实现账户、余额、交易流水、分类、收入/支出/差额、周期比较、大额交易、高频商户和订阅统计。
- [x] 金额统计由 `BillingAnalysisService` 使用 `BigDecimal` 确定性计算，LLM 只负责选择 Tool 和解释。
- [x] 每个 Tool 结果生成 `ExecutionEvidence`；关键数字没有成功证据时由 Grounding Guard 拒绝或替换为确定性摘要。
- [x] 加入真实 Prompt Injection、身份提升、隐藏/幻觉 Tool、虚假成功、循环和 Provider 故障测试。

退出条件：账单分析正常、空数据、越权、注入和幻觉测试全部通过。

### M4：智能转账闭环

- [x] 实现收款人查找、`prepare_transfer` 操作单、挑战绑定、独立确认、AAL2、原子记账和回执。
- [x] 实现余额、币种、账户/收款人状态、日限额、新收款人、高金额和十分钟频率规则。
- [x] 确认时校验参数/摘要哈希，按稳定 UUID 顺序悲观锁账户，并重跑最终风控防止 TOCTOU。
- [x] `Idempotency-Key` 按用户和转账 scope 绑定参数哈希；重放返回同一操作/回执，换参冲突拒绝。
- [x] 完成正常、参数异常、余额不足、AAL2、越权、注入、伪造 ID、参数走私、并发确认和确认重放测试。

退出条件：转账只能通过安全执行链成功；数据库和审计记录保持一致。

### M5：理财、卡片、订阅

- [x] 理财：产品查询、适当性、购买预览、AAL2 确认、订单与持仓。
- [x] 卡片：查询、冻结/解冻、线上/境外开关、限额调整与合法状态机。
- [x] 订阅：确定性周期成本、列表、取消预览、确认和变更历史。
- [x] 所有写操作沿用 Gateway → Prepare → 独立确认 → FINAL_GUARD → 事务提交链；最终执行器不注册为 Tool。
- [x] 覆盖正常、异常、越权、Prompt Injection、幻觉 ID、TOCTOU、幂等/重放和审计场景。

退出条件：每个写用例具备正常、异常、越权、注入、幻觉、幂等和审计测试。

### M6：Agent、多轮对话与跨场景编排

- [x] 实现确定性结构化意图、最多 8 步的任务 DAG、槽位补全、会话状态和中断恢复。
- [x] 组合多个原子 Tool；每轮只暴露就绪节点，查询步骤自动执行，写步骤必须绑定独立确认。
- [x] 实现模型不可用降级、计划非法/越序/多写拒绝、Tool 超时和从 Evidence 恢复。
- [x] 完成账单→订阅优化、余额→理财配置两条黄金路径与真实 Prompt Injection/幻觉回归。

退出条件：至少两条跨场景黄金路径可重复演示，且失败时不产生半完成的隐式副作用。

### M7：演示型前端

- 聊天、多轮上下文、结构化确认卡、风险提示、操作进度和审计解释抽屉。
- 账户/账单/卡片/订阅/理财基础页面；暂不投入视觉美化。
- Playwright 覆盖六类核心演示路径。

退出条件：评委仅通过浏览器即可完成演示并查看“为什么允许/拒绝”。

### M8：部署、压测与比赛交付

- 完善 Docker Compose、初始化、健康探针、脱敏日志、备份/重置脚本。
- 完成并发、故障注入、OWASP API、Prompt Injection 回归和演示脚本。
- 产出演示账号、OpenAPI、架构图、风险规则说明、测试报告和答辩材料。

退出条件：全新机器按 README 可启动；无公网模型时可运行固定演示模式；所有质量门禁通过。

## 全局完成标准

任何模块只有同时满足以下条件才算完成：

- 代码、Flyway 迁移、OpenAPI 和错误码一致。
- 单元测试与集成测试通过；关键业务覆盖正常、异常、越权、Prompt Injection 和幻觉场景。
- 写操作支持幂等、确认过期、确认重放防护和并发安全。
- 允许与拒绝均产生脱敏审计证据，可通过 Trace ID 查询解释。
- LLM 关闭或恶意输出时，仍无法越过 Tool 白名单和确定性安全内核。

## 待评审的关键决策

1. 使用模块化单体而非微服务，先保证安全链路、测试和演示完整性。
2. 当前范围内所有写操作均需确认；高金额、新收款人、解冻等还需模拟 AAL2。
3. 确认通过前不调用领域 Service；确认后再次做所有权、状态、余额和风险复核，防止 TOCTOU。
4. 跨场景默认逐个确认写步骤；只有完整展示所有影响且参数哈希绑定时才允许批量确认。
5. Mock 数据使用固定种子和固定基准日，支持版本校验与一键恢复。
6. 不为写操作配置 `defaultTools`；未来每轮 Agent 请求动态暴露 Allowed Tool Set，执行端再次校验集合成员资格。
7. 永久关闭 Tool resolution fallback 与 ChatClient 自动 Tool Calling；未来由用户控制的循环逐次验证并执行 Tool Call。

## Known risks

- 当前 Spring Boot 管理的 Flyway 版本在日志中声明已测试的 MySQL 上限为 8.1，而 Compose/Testcontainers 使用 MySQL 8.4。当前 5 个 migration、Compose 启动和完整测试均通过；按约束暂不改版本，后续评估升级 Flyway 或固定 MySQL。
- M2 的 Allowed Tool Set 使用进程内随机 HMAC 密钥；应用重启会使尚未过期的旧快照失效。这是安全的 fail-closed 行为，未来多实例部署需改为受管共享密钥或服务端快照存储。
- M3 的对话历史和 `ExecutionEvidence` 当前为进程内、按用户绑定且带 TTL 的存储；重启会丢失，多实例部署需迁移到 Redis/持久化 evidence store。
- 默认环境没有真实 LLM API Key，完整 CI 使用确定性 Fake Model；真实 Provider 的网络、模型行为和费用需要通过独立 `real-llm` profile 验证。
- Grounding Guard 当前验证执行状态、Evidence ID 和关键数字，并提供确定性回退；更强的字段级引用证明和持久化证据链留待后续阶段。
- M4 幂等准备使用数据库唯一约束并在单实例内串行化；当前 Compose 单实例已通过并发确认测试，多实例下的幂等键争用需在部署阶段加入数据库 advisory lock 或专用协调器。
- M5 卡片/订阅/理财准备阶段沿用单实例同步 + 数据库唯一约束；最终确认使用悲观锁和资源版本复核。横向扩容前仍需增加跨实例幂等协调并进行死锁/重试压测。
- M5 适当性结论使用固定演示风险等级和十分钟有效期，只用于虚构沙箱；不等同于真实金融机构的问卷、客户画像或合规审批。
- M6 的计划、步骤状态和恢复 Evidence 当前与对话一样保存在单实例内存中并受 TTL 约束；重启会安全丢失，多实例部署前需迁移到 Redis/持久化计划存储并加入并发版本控制。
- M6 为降低组合写风险，将单个计划限制为最多一个 `PREPARE_WRITE`；多写批次确认尚未实现，用户需拆分任务。
- dev/test AAL2 是固定沙箱码，仅用于比赛演示；没有真实 OTP、设备绑定、尝试次数限流或认证事件审计，绝不能作为生产认证实现。

M6 已完成并停止在本阶段，等待验收；未进入 M7 演示型前端阶段。
