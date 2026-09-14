# 1062 — read_range 回读判定读面

> 来源：J 会话第 62 轮 = effort #1062（[T1579](../../.wayfinder/tickets/T1579-readrange-stats-shape.md) / [T1580](../../.wayfinder/tickets/T1580-readrange-stats-verify.md) / impl 814）。借鉴：S3 TransferManager 分页回读统计（截断率是「拿到的原文不完整」的直接信号）。spill 域第三轴：R53 逐出 / R54 编辑 / R62 回读。

## Problem Statement

`ReadRangeTool`（模型分段回读溢出原文的通道）全路径零计数——完整回读、截断回读、解析失败、skill:// 模式拒绝、未接线拒绝、异常兜底全部静默：**回读行为与截断率不可见**。回读频次是 Spill 管线有效性的下游信号（溢出后模型是否真回来读原文）；截断高发意味着窗口/上限配置与模型实际需要不匹配。

## 目标

- `ReadRangeTool` 增量（spill，静态面）：六 `AtomicLong`。
  - `calls`：call 入口（总桶）；`reads`（完整回读）/ `truncatedReads`（截断回读——单独分桶，管线上限信号）；
  - `parseRejects`（坏 JSON）/ `skillRejects`（skill:// bytes-only 拒绝与未接线拒绝合桶）/ `failures`（catch 兜底）三个拒绝桶。
- 嵌套 `record ReadRangeStats(...)` + `stats()` + `resetForTest()`。
- 守恒恒等式：**calls = reads + truncatedReads + parseRejects + skillRejects + failures**（每入口恰落一桶）。

## 兼容性

纯增量读面：call() 返回语义、窗口解析与 handle 复活联动逐位不变；与 store 层 ReadAuditTrail（审计流水）不同轴共存；静态面理由同族先例；无新配置项。

## Out of Scope

- 按 path 分桶（敏感面——红线纪律）。
- 回读字节量口径（R47 读侧已立，不重复）。
