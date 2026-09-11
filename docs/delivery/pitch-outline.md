# 答辩提纲

## 一句话价值

银盾智行不是让 LLM “拥有银行权限”，而是让它在每轮最小能力、执行端复核、独立确认和可验证审计的边界内协助用户完成任务。

## 建议 6 页结构

1. 痛点：传统银行流程割裂；通用 Agent 又存在注入、幻觉、越权和误操作风险。
2. 方案：六类业务统一自然语言入口，确定性 Banking Sandbox，无真实数据。
3. 架构：动态 Capability → 自控 Tool Loop → Secure Gateway → 独立确认 → FINAL_GUARD。
4. 安全证据：模型可见性不是授权、最终 Service 不是 Tool、owner-scoped 404、幂等/并发/TOCTOU。
5. 演示：账单→取消订阅、余额→适当性→理财、转账确认、注入拒绝。
6. 工程与演进：Compose 一键启动、全栈测试；生产化补真实 MFA、KMS、多实例状态、WAF/限流和合规对接。

## 高频问题

**如果模型被 Prompt Injection 完全控制呢？** 仍只能提议本轮 schema 中的 Tool；Gateway 再检 Allowed Set，参数、资源、风险和确认独立执行。模型不能调用最终写 Service。

**为什么不是只靠 Spring AI 的 Tool 列表？** Tool schema 是模型行为约束，不是安全授权。项目既按请求最小暴露，又在执行端用签名策略快照复核精确 name/version/descriptor。

**如何证明回答不是幻觉？** 关键事实来自 Tool Evidence，金额由 Java 确定性计算；没有证据或没有 `SUCCEEDED` 回执时 Grounding Guard 替换模型说法。

**如何防重复扣款？** 准备和确认均绑定幂等/operation，数据库唯一约束和悲观锁是最后屏障；重放返回同一结果，换参冲突拒绝。

**为何使用模块化单体？** 比赛阶段优先保证事务原子性、可审计性和部署可重复；包边界由 ArchUnit 强制，未来可按 Identity、Safe Execution、Banking Domain 拆分。

**离生产还有什么距离？** 当前 AAL2 为演示码，内存会话计划/证据和单实例协调需要生产化；还需 TLS、KMS、真实 MFA、限流、集中审计、灾备、监管规则和渗透测试。
