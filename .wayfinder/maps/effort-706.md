# effort #706 — MCP 工具目录差异报告

- 会话：G 会话 700 系第 7 轮 ｜ spec [706](../../../docs/spec/706-mcp-directory-diff.md) ｜ 票 [T1012](../tickets/T1012-mcp-dir-diff.md)/[T1013](../tickets/T1013-mcp-dir-diff-verify.md) ｜ impl606
- 借鉴：ArgoCD（≈19K star）diff/plan——期望态 vs 实际态的结构化差异报告；Terraform plan 同思想

## 勘察（排重）

- 600 toolHints() 目录快照+同名注解翻转告警——但「**这次 server 升级到底动了哪些工具**」（整目录 added/removed/翻转的 plan 式报告）无面；告警是事件面，复盘/升级评审要的是差异清单。
- grep DirectoryDiff：零命中。

## 决定

`McpDirectoryDiff`（mcp 纯函数）：diff(baseline, current)→Report——per-server SyncStatus（IN_SYNC/DRIFTED/SERVER_NEW/SERVER_GONE）+ToolChange（ADDED/REMOVED/HINT_CHANGED 带 detail）+**风险方向标记**（readOnly true→false、destructive false→true 为 risky——「工具静默变危险」的评审焦点）+聚合计数。

## 测试

增/删/翻转三类变更+风险标记/server 新增消失/全同 IN_SYNC/null fail-fast。

## 诚实边界

对比口径=hint 三布尔+title（工具描述/入参 schema 不在快照内——600 同边界）；纯函数无状态（基线由调用方供给——周期快照+diff 的编排归宿主/后续接线轮）。
