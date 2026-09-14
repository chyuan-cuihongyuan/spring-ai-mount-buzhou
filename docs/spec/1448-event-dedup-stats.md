# 1448 — 事件去重聚合读面

> 来源：L 会话第 48 轮 = effort #1448（票 T2195 / T2196 / impl 1100）。**换题记录**：R48 原题 FeedbackExporter 画像面过窄——换入事件去重聚合轴。借鉴：SendGrid/Mailgun webhook replay 遥测（重复事件注入压力=去重率直接显形）。

## Problem Statement

`EventDeduplicator`（webhook 出站事件去重：指纹环 1024）只有全局 `buzhou.event.deduped` counter：**放行/去重计数、去重率、环占用**无聚合读面——重复事件注入压力（上游重试风暴/同 payload 高频派发）静默。

## 目标

- `EventDeduplicator`（core/webhook，实例面）增量：
  - `passedCount`/`dedupedCount` 双计数（守恒 seen = passed + deduped）；
  - `deduplicationStats()` → `record DeduplicationStats(passed, deduped, ringSize, capacity)` + 派生 `deduplicationRatio()`（0 总量 -1 哨兵）；
  - `resetStatsForTest()`：只清计数（环/成员状态保留——去重语义不被测试复位破坏）。
- onEvent 去重/放行/环驱逐语义逐位不变（只增记账）。

## 兼容性

纯增量读面：去重判定（type+排序 payload sha256 指纹）/环驱逐/delegate 转发语义逐位不变。

## Out of Scope

- 按事件类型分桶去重率（基数红线）。
- 指纹环容量动态调整（既有语义）。
- 跨去重器聚合（多实例各自记账——口径显式）。
