# Spec 3007 — 混合逻辑时钟（effort #3007，R8）

> wayfinder map：`.wayfinder/maps/effort-3000.md`（T5015–T5016，impl 2008）。
> 借鉴：Hybrid Logical Clock（Kulkarni 2014 / CockroachDB）。

## Problem Statement

墙钟时间戳可比可读但会回拨/漂移（同毫秒并发不可分序、倒拨破坏
因果）；Lamport 纯逻辑计数器保因果但与人读时间脱钩、多实例不可
比。需要既有物理语义又有因果保证的时间戳。

## Solution

`HybridLogicalClock`（core/concurrent，单事件线程口径）：

- 时间戳 `Hlc(wall, counter)`（墙先比、计数后比）；
- `tick()` 本地事件：wall'=max(wall, pt)，同墙计数 +1 / 墙进清零
  ——严格单调；
- `observe(remote)` 接收合并：wall'=max(wall, pt, remote.wall)，
  三路同墙 max(counter)+1 / 两路同墙在侧 +1 / 全新墙清零——
  远端超前吸收为基线、落后无感；
- 物理钟 LongSupplier 注入（测试确定性）；单调不倒（回拨吸收）；
  因果保持（happens-before ⇒ 严格小于；反向不成立——真并发判
  别归向量时钟）。

## User Stories

1. 作为事件作者，时间戳人读可比（贴物理时间）且因果安全（不倒拨）。
2. 作为分布式作者，跨实例互投消息吸收远端超前——免全序协调。

## Testing Decisions

- 固定物理钟下百次 tick 同墙严格递增；墙跳清零仍单调；远端超前
  (500,3) 吸收为 (500,4) 且后续贴新基线；三路同墙 max+1 双向；
  远端落后无感 (100,2)；物理回拨不倒；因果跨钟传递
  tick→observe→tick 严格小于；双钟交错互投 50 轮每步严格增；
  Hlc 序墙先计后四路。

## Out of Scope

- 不做真并发判别（CONCURRENT 态归 VectorClockOrder）；不做持久化
  恢复语义；不做 NTP 漂移界分析。

## Further Notes

- 与 VectorClockOrder（因果三态）/ SequenceOrder（回绕序号）成
  序刻画三件：混合序 / 向量因果序 / 回绕数值序。
- 里程碑：8/150。
