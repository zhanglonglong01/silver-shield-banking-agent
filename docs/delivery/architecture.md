# 交付架构与信任边界

## 部署拓扑

```mermaid
flowchart LR
    B[评委浏览器] -->|HTTP :3000| W[Vue 3 + Nginx]
    W -->|REST| A[Spring Boot 3 / Java 17]
    A -->|业务事实、操作单、审计| M[(MySQL 8.4)]
    A -->|不透明会话| R[(Redis 7.4)]
    A --> P{BankingModelClient}
    P -->|默认| O[固定离线 Provider]
    P -.可选显式配置.-> L[Spring AI / OpenAI-compatible]
```

四个容器均带健康探针。浏览器只访问 Nginx；Nginx代理 `/api`、`/v3/api-docs` 和健康检查。数据库和 Redis 不向 Agent 或模型暴露接口。

## 安全执行链

```mermaid
sequenceDiagram
    actor U as 用户
    participant A as Agent Loop
    participant P as Capability Policy
    participant M as Model
    participant G as SecureToolGateway
    participant S as Domain Service
    participant D as MySQL/Audit
    U->>A: 自然语言 + 服务端会话
    A->>P: Principal、AAL、候选能力
    P-->>A: 签名 AllowedToolSetSnapshot
    A->>M: 仅本轮允许的 definition-only schemas
    M-->>A: 不可信 Tool Call 提议
    A->>G: call + 原策略快照
    G->>G: 成员复核、Registry、参数、权限、风险
    alt 只读
        G->>S: owner-scoped query
        S->>D: 查询本人事实
    else 写操作
        G->>D: 仅创建操作单和确认挑战
        G-->>U: 不可变摘要 + requiredAal
        U->>S: 独立确认端点
        S->>S: FINAL_GUARD + 锁 + 幂等
        S->>D: 原子业务提交 + Audit
    end
```

## 强制边界

- Tool 可见性不是授权：模型看不到某 Tool 只是第一层降噪；Gateway 执行时再次验证同一 Allowed Tool Set。
- 不使用 `defaultTools`，Tool resolution fallback 和 ChatClient 自动 Tool Calling均显式关闭。
- Agent 禁止依赖 Repository、Tool Handler 或 Registry；Tool Handler 禁止依赖 Repository。ArchUnit 扫描真实类，避免空包规则 vacuous pass。
- 最终写 Service 不注册为 Tool。对话中的“确认”不能消费 challenge。
- 身份、角色、AAL 和 `userId` 来自服务端认证上下文，模型参数中不存在这些授权字段。
- MySQL 是业务事实源；模型文本、对话历史、Redis 和前端状态都不是授权事实。
