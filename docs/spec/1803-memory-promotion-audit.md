# Spec 1803 — 记忆层代晋升审计（effort #1803，R4）

> wayfinder map：`.wayfinder/maps/effort-1800.md`（T2807–T2808，impl 1404）。借鉴：
> JVM 分代 GC 晋升诊断——晋升率与**过早晋升**（premature promotion，年轻代
> 缓冲失效全数直奔老年代）是容量错配的经典信号。

## Problem Statement

记忆分层（微压缩 → 九段摘要 → 归档）只答「每层做了多少」，答不了「分层
配置得对不对」：晋升率常高 = 微压缩没拦住短命内容，摘要被灌水；过早晋升
周期堆积 = 年轻代缓冲形同虚设。现状没有层间流动的体检读数。

## Solution

`MemoryPromotionAudit`（buzhou-memory，静态纯函数）：

- `CycleFacts(microCompacted, promotedToSummary, archivedDirect, retainedInPlace)`
  单周期事实（紧凑构造器核契约：非负 + 三去向之和 = 产出）；
- `analyze(cycles)` → `PromotionReport(cycles, 四总计, prematurePromotionCycles)`：
  过早晋升 = 有产出且零原地保留的轮；
- `promotionRate()` / `directArchiveRate()`（无产出 -1 哨兵）。

## User Stories

1. 作为记忆策略维护者，promotionRate=0.9 + 过早晋升 8/10 轮 → 微压缩阈值
   该收紧（年轻代没起过滤作用），而不是继续加摘要预算。
2. 作为审计者，directArchiveRate 高 → 大量内容越级冷存，回读路径变长，
   该核对归档判据。
3. 作为框架宿主，周期口径（轮界/压缩 tick）自声明，纯读面零侵入。

## Implementation Decisions

- 纯读面零状态，不动 MicroCompactionPolicy 判定；只读不裁决。
- 去向唯一性在构造器核（fail-fast 先于账目）；null 按空表。

## Testing Decisions

- 聚合账目 + 过早晋升只计零保留产出轮（空转轮不计）；健康/全过早两极；
  空表/null 哨兵；畸形（负数/去向失恒）fail-fast。

## Out of Scope

- 不自动调整分层阈值；不接 Micrometer。

## Further Notes

- 与 CompactionRatioStats 正交：那是「压缩比」，这是「层间流动率」。
