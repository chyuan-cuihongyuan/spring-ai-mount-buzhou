# 706 — MCP 工具目录差异报告

> 来源：G 会话第 7 轮 = effort #706（600 目录快照的 plan 式差异深化）/ [T1012](../../.wayfinder/tickets/T1012-mcp-dir-diff.md) / [T1013](../../.wayfinder/tickets/T1013-mcp-dir-diff-verify.md) / impl 606。

## Problem

600 让 server 自报注解进入目录快照并对同名翻转告警——但告警只告诉你「X 工具 readOnly 翻了」，不告诉你「这次 server 升级一共动了什么」。升级评审/复盘需要的是 plan 式差异清单：新增了哪些工具、删了哪些、哪些注解翻转、其中哪些是**危险方向**（readOnly→false、destructive→true）。「工具静默变危险」是供应链攻击的经典信号，散在逐条告警里看不出来。

## Solution

ArgoCD diff/Terraform plan 思想（≈19K star）：

- `McpDirectoryDiff`（mcp，纯函数静态原语）：
  - `diff(baseline, current)` → `Report`；
  - per-server：`ServerDiff(server, status, changes)`——status ∈ IN_SYNC / DRIFTED / SERVER_NEW（仅 current 有）/ SERVER_GONE（仅 baseline 有）；
  - `ToolChange(server, tool, kind, detail, risky)`——kind ∈ ADDED / REMOVED / HINT_CHANGED；detail 形如 `readOnlyHint: true→false`；risky=true 当 readOnly true→false 或 destructive false→true（危险方向翻转——评审焦点）；
  - 聚合：`added / removed / hintChanged / risky` 计数；
  - 对比口径=快照内的 title+三 hint（与 600 同边界——工具描述/入参 schema 不在自报快照内）。

## User Stories

1. 升级评审：server 升级前后各取一次 toolHints() 快照，diff 即见 plan——「新增 2 工具、其中 1 个 destructiveHint=true」一屏定夺放行与否。
2. 复盘：risky 计数非零 = 有工具向危险方向翻转——供应链审计的直接证据行。

## Implementation Decisions

- 纯函数无状态：baseline 由调用方供给（上次快照）——周期快照编排归宿主/后续接线轮，本面先把对比语义立住。
- 排序确定性：server 名/工具名字典序——快照断言可复现。
- HINT_CHANGED 的 detail 逐字段列出（title/readOnly/destructive/idempotent），只列变化字段。

## Testing Decisions

- 三类变更+危险翻转标记（readOnly true→false risky；title 变化不 risky）。
- SERVER_NEW/SERVER_GONE/IN_SYNC 三态。
- null map fail-fast；空快照 vs 空快照 = 空 Report IN_SYNC 无条目。

## Out of Scope

- 工具描述/入参 schema 对比（不在自报快照——600 同边界）。
- 周期快照编排与告警接线（600 事件面已有翻转告警；本面是查询式 plan）。

## Further Notes

与 R1/R3/R5 同主题线：**事件面 → 证据面**。600 是「翻转瞬间告警」，本面是「拿两份快照出一份评审报告」。
