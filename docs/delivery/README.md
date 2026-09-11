# M8 比赛交付索引

本目录是“银盾智行——面向个人银行业务的安全可信智能体”的比赛演示包。项目默认使用 `offline-demo` profile，不需要公网、模型 API Key 或真实银行数据。

| 材料 | 用途 |
|---|---|
| [demo-script.md](demo-script.md) | 8 分钟现场演示顺序、台词和验收点 |
| [architecture.md](architecture.md) | 系统拓扑、信任边界和写操作闭环 |
| [security-case.md](security-case.md) | Capability、风控、确认、幂等和可解释拒绝 |
| [owasp-api-mapping.md](owasp-api-mapping.md) | OWASP API Security Top 10 防护映射 |
| [operations-runbook.md](operations-runbook.md) | 冷启动、健康检查、reset、备份和故障排查 |
| [test-report.md](test-report.md) | M8 自动化与部署验收结果 |
| [pitch-outline.md](pitch-outline.md) | 答辩陈述和高频问题回答 |

公开 API 契约在运行时由 `GET /v3/api-docs` 提供，交互文档为 `/swagger-ui.html`。使用 `scripts/export-openapi.ps1` 可将当次运行的契约导出到未提交的 `artifacts/openapi.json`。

演示数据声明：所有姓名、账号、卡号、商户、交易、订阅和理财产品均由固定 seed 的程序生成，纯属虚构，与任何真实个人或金融机构无关。
