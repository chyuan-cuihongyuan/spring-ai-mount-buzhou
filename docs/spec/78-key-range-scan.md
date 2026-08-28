# Spec 78 — 键序区间扫描 SPI（effort #39）

> wayfinder map：`.wayfinder39/MAP.md`（T309–T310）。#35 fog「outbox due-time 键序
> 结构」前置半场。借鉴：Kafka log 有序读 / LSM 区间扫描。

## Problem Statement

SessionStateStore 只有 scanByPrefix：无顺序保证、无上界、无 limit——「时间编进键」
的结构（如 outbox due-time 索引）只能全量前缀读再内存过滤，退化为每轮全表扫。

## Solution

`scanByKeyRange(sessionId, prefix, fromKeyInclusive, toKeyExclusive, limit)`：键字典序
升序 + 含界下界（null = 前缀起点）+ 排他上界（null = 无界）+ limit 截断（≤0 = 空）。
默认实现 = scanByPrefix + TreeMap 排序（正确性兜底，全量读）；JDBC 覆写
`ORDER BY state_key LIMIT`（区间条件下推）；内存覆写键迭代免全值拷贝。

## User Stories

1. 作为 outbox 调度器，我要按 due 时间键序取最早到期 N 条，所以退避积压不放大读。
2. 作为 store 实现者，我要默认实现保正确，所以覆写是优化不是正确性前提。
3. 作为红队，我要三栈同测同一契约，所以语义不漂移。

## Implementation Decisions

- from=null = 前缀起点（prefix 内语义，非全键空间）。
- 顺序保证是与 scanByPrefix 的本质差异（契约 containsExactly 断言）。
- Redis 暂走默认实现（ZRANGEBYLEX 需换结构——fog 记账另议）。

## Testing Decisions

- 契约测试（AbstractBuzhouStoresContractTest，in-memory/H2 继承）：键序升序、
  排他上界、含界下界、limit 截断、空键空间与非法 limit。

## Out of Scope

- value 谓词下推；Redis 有序结构覆写；反向扫描（无需求证据）。

## Further Notes

- 消费方：spec 79（outbox due-time 索引）——本轮是独立可验收的底座半场。
