# 1014 — Spotlighting 应用与损坏计数读面

> 来源：J 会话第 15 轮 = effort #1014（[T1479](../../.wayfinder/tickets/T1479-spotlighting-stats-shape.md) / [T1480](../../.wayfinder/tickets/T1480-spotlighting-stats-verify.md) / impl 767）。与 R3/R14 同族：**静默行为显形**（安全控制覆盖率 + 篡改探测）。

## Problem Statement

Spotlighting（spec 11，MSRC 提示防御：随机分隔符 + 告示 + 交织标记）是纯静态工具类零计数：①防御是否真的在生效（wrap 调用了多少次）不可见；②`unwrap` 对「含标记头但结构不完整」的串**原样放行**（无 END/无体——截断或篡改的包裹）且无任何信号——安全控制的失效静默发生。

## 目标

- 新公共 record `SpotlightingStats(long wrapped, long unwrapped, long malformed)`（core.hook，api 面）。
- `Spotlighting` 进程级静态三计数：`wrapped`（unwrap 入口含 BEGIN_HEAD 即计尝试）/ `unwrapped`（成功还原）/ `malformed`（含头但结构不完整原样放行）——守恒不变量 **wrapped == unwrapped + malformed**。
- `stats()` 快照 + `resetForTest()` 测试隔离注入点（静态进程态，BuzhouMetricsHolder 先例）。
- 行为逐位不变：计数不改变任何返回值；明文（无头）串不计（非包裹语义）。

## 兼容性

纯增量读面：wrap/unwrap/stripMark 返回值逐位不变；AtomicLong 热路径可承受（wrap 每工具结果一次）。

## Out of Scope

- 按工具/tag 分桶（基数纪律）。
- malformed 升级 WARN（安全事件语义另议；计数已给装配层自警依据）。
