# 936 — ObservabilityStore 契约校验套件

> 来源：I 会话第 36 轮 = effort #936（[T1315](../../.wayfinder/tickets/T1315-obs-contract-shape.md) / [T1316](../../.wayfinder/tickets/T1316-obs-contract-verify.md) / impl 688）。spec 922/929/930 契约系列收口（核心 SPI 契约全覆盖）。

## 背景

`ObservabilityStore`（span/event/injectionSnapshot 三存储 + 摘要列表）是观测管线的数据面 SPI——第三方实现（JDBC/OLAP 后端）的「会话隔离、保序、空读、删除幂等」语义无自证工具。

## 目标

- `ObservabilityStoreContract`（spi 包静态 verify 范式）八项检查：
  1. saveSpans 后 spansOfSession 保序返回；
  2. saveEvents 后 eventsOfSession 保序返回；
  3. injectionSnapshot 写读一致（sessionId+turnSeq 键）；
  4. 未知会话 span/event 空读；
  5. 未知 turnSeq 快照 empty；
  6. 跨会话隔离（s1 写入不影响 s2）；
  7. deleteSession 后读空且幂等；
  8. eventsOfSpan 按 spanId 过滤。
- core 测试域 `ObsContractAccessTest`：内存实现过八项。

## 兼容性

纯增量：新公共契约类 + 测试；零既有行为变化。
