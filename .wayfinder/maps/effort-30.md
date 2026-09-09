# Wayfinder Map — Buzhou 边界机会压缩（effort #30）

> effort #30，延续 #5–#29（累计 176 轮 / T1–T290 / impl 1–215）。
> 主线：**边界机会压缩**——研究 Tier2 借鉴项「Letta：在自然边界压缩」：现状只在预算
> 压力下压缩（触发逐出梯子——有损压力下生成摘要，质量降）。本地裁定：语义边界检测
> （embedding 漂移）fog 留位，先落 **Completed-Turn 边界代理**：积压超阈值时在干净
> 轮边界提前增量摘要（预算尚宽松——摘要质量更高、避免梯子紧急态）。

## Destination

`buzhou.memory.boundary-compact-backlog=N`（默认 0=关）：待摘积压（turnSeq ≤ cutoff
且 > coversUpTo 且未摘）≥ N 时提前走摘要路径（即便预算未压）；无摘要模型/熔断开路时
跳过（与既有降级同口径）；默认关零变化；新键 1 个登记。

## Notes

- 诚实边界：Completed-Turn 是自然边界的结构代理（真语义检测 fog）；提前摘要花一次
  摘要调用换后续梯子豁免——成本口径入 runbook。

## Decisions so far

- 触发点在预算判定前（backlog 计算复用既有 toSummarize 口径）；阈值默认 0=关。

## Not yet specified

- 语义漂移检测（embedding）；backlog 按字符/token 加权。

## Out of scope

- 沿用 #7–#29；新查询面；新配置组。

## Tickets

- [x] [T291 backlog 阈值触发 + yml 键](../tickets/T291-backlog.md)（impl-216）
- [x] [T292 红队（提前摘要/未达阈值不提前/默认零变化）+ 文档矩阵 + 收口](../tickets/T292-backlog-close.md)
