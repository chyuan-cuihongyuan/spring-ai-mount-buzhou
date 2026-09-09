# Wayfinder Map — Buzhou 键序区间扫描 SPI（effort #39，50 轮自迭代第 4 轮）

> effort #39，延续 #38（T307–T308 / impl-224）。主线：**#35 fog 项「outbox due-time
> 键序结构」的前置半场**——SessionStateStore 只有 scanByPrefix（无序、无界），
> due-time 索引需要「键序即时间序」的有序区间读；先把 SPI 能力铺好，outbox 换轨
> 在下一轮（两轮可独立验收）。

## Destination

`SessionStateStore.scanByKeyRange(sessionId, prefix, fromKeyInclusive, toKeyExclusive,
limit)`：键字典序升序、含界下界（null=前缀起点）、排他上界（null=无界）、limit
截断（≤0=空）；默认实现全量读排序（正确性兜底），JDBC 覆写 ORDER BY+BETWEEN+LIMIT
下推、内存覆写键迭代；契约测试三栈同测（in-memory + H2；MySQL/PG 按档案）。

## Notes

- 借鉴：Kafka log 按偏移有序读 / LSM 索引区间扫描；键序结构是 outbox due-time
  索引（spec 79）与任何「时间编进键」结构的公共底座。
- Redis 暂走默认实现（ZRANGEBYLEX 需换数据结构——fog 记账，不预设）。

## Decisions so far

- from=null 语义 = 前缀起点（非全键空间起点——prefix 内才有意义）。
- 顺序保证是本方法与 scanByPrefix 的本质差异（契约显式断言 containsExactly）。

## Not yet specified

- outbox due-time 索引换轨（#40 候选——本轮 SPI 的消费方）；Redis 键序结构覆写。

## Out of scope

- 沿用 #7–#38；value 侧谓词下推（无需求证据）。

## Tickets

- [x] [T309 scanByKeyRange SPI + JDBC/内存覆写](../tickets/T309-key-range-spi.md)（impl-225）
- [x] [T310 契约红队（键序/上下界/limit/空集）+ 文档收口](../tickets/T310-key-range-close.md)
