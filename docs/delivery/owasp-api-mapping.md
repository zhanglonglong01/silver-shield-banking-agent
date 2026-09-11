# OWASP API Security Top 10 映射

| 风险 | 项目控制 | 自动化/验收证据 |
|---|---|---|
| API1 Broken Object Level Authorization | 所有资源按 `principal.userId` 查询；他人 ID 与不存在 ID 均返回 404 | Alice 读取 Bob 账户/卡片/订阅/操作单拒绝测试与 `acceptance.ps1` |
| API2 Broken Authentication | Redis 不透明 bearer、SHA-256 server key、TTL、logout revoke、锁定用户拒绝 | login/logout/revoke 与未认证 401 测试 |
| API3 Broken Object Property Level Authorization | DTO 白名单、Jackson 未知/重复字段拒绝；请求不接受 userId/role/AAL | 参数走私、额外字段和类型错误测试 |
| API4 Unrestricted Resource Consumption | 消息/参数/日期/分页限制；Agent 轮数、单轮调用、总调用、上下文和超时上限 | limit、loop、timeout、oversize 测试；并发烟测 |
| API5 Broken Function Level Authorization | Capability/角色/AAL + Gateway 二次成员检查；reset 仅 DEMO_ADMIN 且 prod 不装配 | Tool 未暴露仍拒绝、admin/customer 边界、prod reset 404 |
| API6 Unrestricted Access to Sensitive Business Flows | 写操作 PREPARE/确认分离、AAL2、速度规则、幂等和确认单次消费 | 重放、双击并发、频率、AAL2 与 FINAL_GUARD 测试 |
| API7 Server Side Request Forgery | 没有 URL/HTTP/文件读取 Tool；Registry 为编译期显式对象 | 未注册/混淆 Tool 测试和 ArchUnit |
| API8 Security Misconfiguration | prod 禁 reset/模拟 AAL2；自动 Tool Calling/fallback 关闭；真实密钥不写入仓库；演示默认凭据仅限 loopback Sandbox；错误不含堆栈；CSP/安全 header | 配置锁定、SecretHygiene、prod profile、浏览器测试 |
| API9 Improper Inventory Management | OpenAPI 运行时生成；Tool 使用精确语义版本和 descriptor digest；Flyway 管理 schema | OpenAPI 配置测试、旧 Tool 版本拒绝、迁移测试 |
| API10 Unsafe Consumption of APIs | LLM 与 Tool Result 均视为不可信；schema、证据锚点、Grounding Guard | Prompt Injection、幻觉 Tool/ID/数字/成功声明测试 |

此映射是比赛范围的控制说明，不等同于生产渗透测试或合规认证。公网部署仍需 TLS、WAF/API gateway、集中密钥管理、真实 MFA、限流与第三方安全评估。
