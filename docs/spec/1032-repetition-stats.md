# 1032 — 打转检测触发聚合读面

> 来源：J 会话第 33 轮 = effort #1032（[T1517](../../.wayfinder/tickets/T1517-repetition-stats-shape.md) / [T1518](../../.wayfinder/tickets/T1518-repetition-stats-verify.md) / impl 785）。与 R28/R30 同族：逐次判定 → 跨调用聚合水位（LLM 打转频率 = 提示词/温度/stop 序列调参依据）。

## Problem Statement

RepetitionDetectorHook（spec 326）只有 per-session currentRun 即时读数 + FIRED micrometer 计数器，无进程内聚合快照：打转 fire 总次数、unstick 实际拦截次数、历史最大 run 长不可直读——检测阈值（window/相似度）调参与「打转是否真的在发生」的量化评估无据。

## 目标

- `RepetitionDetectorHook` 增量（core/runaway，实例级）：`fires`（verdict 触发数，含 observe-only）/ `blocks`（unstick 实际拦截数）/ `maxRunSeen`（verdict runLength 峰值，CAS 只增不降）三 AtomicLong。
- 嵌套 record `RepetitionStats(long fires, long blocks, int maxRunSeen)` + `stats()` 快照。
- afterModel 行为逐位不变（observe-only 默认 CONTINUE；unstick block 文案不变）。

## 兼容性

纯增量读面；无新配置项。

## Out of Scope

- 闩住后 run 继续延长的持续追踪（verdict 时刻值即足——持续 run 长可由 currentRun 即时读）。
- 按 sessionId 分桶（会话数有界 1024 但聚合粒度无必要）。
