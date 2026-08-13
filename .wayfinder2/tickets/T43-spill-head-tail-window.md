---
id: T43
title: spill · head+tail 窗口回读风味 + 显式中段标记
type: task
status: closed
assignee: ""
blocked-by: T28
created: 2026-08-14
---

## Question

模型回读大输出时常要「开头+结尾」（schema 在头、结论在尾）——如何提供窗口风味而不重蹈 Codex 覆辙？事实源：openai/codex（105,721★，**反面教材+风味源**：v0.24.0 起 256 行或 10KiB 先到先截、策略=**头 128 行+尾 128 行掐中间**、无省略量标记；社区批评 #6426/#5913；v0.56+ 转 `tool_output_token_limit`）。

## 待定决策（研究推荐已备）

1. `ReadRangeTool` 的 `mode=byte` 增 `window=head|tail|head_tail` 风味参数（`headBytes`/`tailBytes` 默认对称）——采纳。
2. 中段以**显式标记行**替代：`…[omitted N bytes, offset X..Y; refetch via mode=byte]`——采纳（与 Codex 本质差异：销毁式截断 vs 原始字节在 spill 存储完整保留、可无损回取）。
3. 与既有显式截断标记（T20 落地）统一格式——spec 定。

依据：`docs/research/oss-perfect-tier23.md` §4.1（工作量小，ROI 高：最便宜增量）。

## Resolution

**Ratify 推荐 → [docs/spec/12-perfect-adoption.md](../../docs/spec/12-perfect-adoption.md) §spill-15**（用户常设授权 2026-08-14 ratify、可推翻）。window=head|tail|head_tail+显式中段省略标记（与 T20 截断标记统一格式）；原始字节永不销毁。
