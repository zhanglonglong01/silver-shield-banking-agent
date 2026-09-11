# 安全与可解释执行论证

## 一个判断，三层证据

系统要回答的不只是“做了什么”，还包括“为什么允许”和“为什么拒绝”。证据分为：Capability 快照证明当时允许模型提议什么；Tool Security Audit 证明参数、注册表和执行端成员检查发生过；Operation/Risk Audit 证明写操作为何需要确认、AAL2 或被拒绝，以及最终 Service 是否被调用。

## Capability 模型

输入为服务端 Principal、角色、AAL、requestId、渠道、候选 Tool 和策略版本。输出是短期、不可变、HMAC 完整性保护的 `AllowedToolSetSnapshot`。每个成员绑定精确 `(name, version, descriptorDigest, accessMode, requiredCapability)`。快照过期、身份/AAL 错绑、摘要变化、HMAC 篡改或 Tool 不在集合内均 fail closed。

## 写操作状态机

```text
输入 → PREPARE 风控 → DENIED / AWAITING_STEP_UP / AWAITING_CONFIRMATION
     → 独立确认 → FINAL_GUARD → SUCCEEDED / DENIED / EXPIRED / CANCELLED
```

准备阶段不修改余额、卡状态、订阅或持仓。确认绑定 `userId + operationId + argsHash + summaryHash + expiry`；确认端点不接受金额、收款人等业务字段。最终复核重新读取资源并加锁，防止 TOCTOU。相同幂等键同参数返回原操作/回执，换参冲突拒绝。

## 主要风险规则

| 场景 | 允许/升级条件 | 拒绝原因示例 | 为什么 Service 不会误执行 |
|---|---|---|---|
| 转账 | 本人 ACTIVE 账户与收款人、币种一致、余额/日限额/速度通过；高金额或非可信收款人需 AAL2 | `INSUFFICIENT_FUNDS`、`DAILY_LIMIT_EXCEEDED`、`VELOCITY_LIMIT_EXCEEDED` | PREPARE 只建操作单；确认时重跑全部规则并锁账户 |
| 卡片 | 冻结/收紧可 AAL1；解冻/放宽需 AAL2；状态转换合法 | `CARD_TRANSITION_INVALID`、`CARD_LIMIT_INVALID`、`RESOURCE_VERSION_CHANGED` | 状态与控制由独立命令和版本号绑定 |
| 订阅 | 本人 ACTIVE 且可取消，展示取消影响 | `SUBSCRIPTION_NOT_CANCELLABLE`、`ALREADY_CANCELLED` | 确认前订阅状态不变，确认后写变更历史 |
| 理财 | 产品在售、期限有效、画像未过期、风险匹配、起购/递增/余额通过；购买固定需 AAL2 | `RISK_LEVEL_MISMATCH`、`BELOW_MINIMUM_PURCHASE`、`SUITABILITY_BINDING_MISMATCH` | 适当性 ID 精确绑定用户、产品、账户、金额和币种 |
| 所有 Tool | Registry 精确命中、schema 严格、当前快照含该 Tool、资源归本人 | `TOOL_NOT_REGISTERED`、`ARGUMENTS_INVALID`、`RESOURCE_NOT_AVAILABLE` | 任一前置门禁失败即终止，审计记录为 DENY/ERROR |

## Prompt Injection 与幻觉

离线 Provider 可识别常见身份/规则覆盖指令，但安全性不依赖关键词：即使模型完全被诱导，隐藏 Tool 不在 schema 中，Gateway 仍拒绝；伪造 UUID 在 owner-scoped UseCase 失败；额外 `userId`/`instructions` 被严格 schema 拒绝；虚假成功声明没有 `SUCCEEDED` 回执时由 Grounding Guard 改写。模型既没有数据库凭证，也没有任意网络、SQL 或代码执行 Tool。

## 日志与隐私

每个 HTTP 响应携带 UUID `X-Trace-Id`。日志仅记录 method、URI path、status 和耗时，不记录 query、body、Authorization、密码、AAL2 code 或 Prompt。Tool audit 只保存规范化参数 SHA-256，不保存原始参数。账户和卡片只使用掩码；本项目没有真实银行数据。
