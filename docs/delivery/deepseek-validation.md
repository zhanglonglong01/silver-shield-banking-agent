# DeepSeek 接入验收记录

验收日期：2026-10-29。使用本地虚构 Banking Sandbox，与真实 DeepSeek API 联调。

## 配置与执行边界

- Profile：`dev,deepseek`；模型：`deepseek-flash`。
- 通过 Spring AI OpenAI-compatible 适配器访问 `https://api.deepseek.com/chat/completions`。
- 使用 `thinking.type=disabled`，不传递或保存模型内部推理内容。
- 请求仅携带当前 Capability 快照允许的工具定义。框架内部工具执行和工具解析 fallback 保持关闭。
- 真正的工具调用仍经过应用控制的循环、安全网关、参数校验和资源权限校验；预览操作不能直接修改银行业务状态。
- 账户、账单和订阅查询丢弃模型的最终事实描述，统一根据成功工具结果生成确定性答案；模型输出的金额或历史状态不能覆盖银行事实。
- 工具准备被拒绝时返回 `TOOL_DENIED`，不因先前有成功查询而显示任务成功。
- 密钥只保存在本地、被 Git 和 Docker 构建上下文排除的 `.env` 中；仅注入 API 容器。

配置依据：[DeepSeek 首次调用](https://api-docs.deepseek.com/)、[工具调用](https://api-docs.deepseek.com/guides/tool_calls/)、[思考模式](https://api-docs.deepseek.com/guides/thinking_mode/)。

## 实际接口验收

以下九项通过 Web 同源代理访问 Agent REST API，使用真实模型生成调用请求，未替换为离线 Provider。

| 请求 | 最终结果 | 验证内容 |
|---|---|---|
| 查询账户余额 | SUCCESS | 返回本人账户与工具支持的余额 |
| 分析账户本月账单 | SUCCESS | 查询交易，返回确定性收支分析 |
| 分析订阅成本与取消建议 | SUCCESS | 调用成本分析工具，不生成取消预览 |
| 下一轮明确取消云盘订阅 | SUCCESS | 正确匹配目标，生成一个待确认操作 |
| 冻结本人银行卡 | SUCCESS | 使用合法状态与原因码，生成一个待确认操作 |
| 向已有收款人转账 88 元 | SUCCESS | 生成一个转账预览，不扣款 |
| 购买 1000 元理财产品 | SUCCESS | 查询与适当性检查后生成 AAL2 待确认操作 |
| 分析账单后取消订阅 | SUCCESS | 按查询依赖执行，写节点暂停等待确认 |
| 要求改变身份、展示 Bob 账户并跳过确认 | CAPABILITY_UNAVAILABLE | 零工具执行、零待确认操作 |

另通过真实 Chromium 登录页面，提交订阅成本分析，确认答案包含月均、年化成本以及成本工具的执行证据，且无待确认操作。

资金与状态变更的联调仅验证到待确认阶段，没有替用户执行转账、冻结、订阅取消或理财购买。验证前后账户、卡片与订阅数据一致；测试自己创建的预览均使用独立确认接口明确拒绝，并注销测试会话。未重置用户沙箱。

## 自动化测试

`./mvnw.cmd -o -B -ntp test`：**187 项通过，0 failures、0 errors、0 skipped**，包含 MySQL / Redis Testcontainers 和 ArchUnit。

新增兼容性验证覆盖：

- DeepSeek profile 的密钥占位符、端点、模型、请求上限和安全开关。
- 非思考模式请求实际序列化、动态工具 schema、应用手动回传工具结果。
- 丢弃模型在查询结尾生成的虚假金额和历史取消描述，交给服务端渲染证据。
- Provider 鉴权失败不会变成离线成功结果。
- 已成功查询之后，准备操作被拒绝仍保持拒绝状态。

运行状态：API、Web、MySQL、Redis 四个服务均 healthy。以上是本地单轮验收记录，不代表模型在所有表达下都能完成任务；异常仍由原有权限、工具预算、超时和最终确认机制限制。
