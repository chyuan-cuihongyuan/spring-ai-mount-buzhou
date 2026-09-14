# 1415 — 事件总线积压水位读数

> 来源：L 会话第 16 轮 = effort #1415（票 T2131 / T2132 / impl 1068）。借鉴：Kafka consumer lag（积压水位先于丢数显形——lag 拉大是容量预警，不是事后从丢弃日志倒推）。

## Problem Statement

`EventBusStats.queueDepth` 是瞬时值：「队列最深到过多少、发生过多少次限时阻塞推入」无历史面。BufferedEventDispatcher 的 BLOCK 策略首推失败即进限时等待——背压已发生但无信号；水位逼近容量 = 丢弃将至的预警，`EventDispatchConfig.capacity` 规划无数据可依。

## 目标

- `EventBackpressureStats`（core/session，进程级静态读面，ToolArgsValidator/StructuredOutputStats 同款先例——埋点在 internal 分发器，读面归公共类）：
  - `depthWatermark`：队列深度历史峰值（跨会话进程级——同一 JVM 总量治理视角；按会话分桶属基数红线）；
  - `blockedPushes`：BLOCK 策略限时等待推入累计（>0 即容量曾被打满）；
  - `stats()` 嵌套 `record Snapshot(depthWatermark, blockedPushes)` + `resetForTest()`。
- 埋点：BufferedEventDispatcher 入队路径三处深度采样（direct offer 成功 / BLOCK 时限等待返回 / DROP_OLDEST 挤旧后）+ BLOCK 首推失败一处——只增记账，行为逐位不变。

## 兼容性

纯增量读面：入队/溢出策略/丢弃计数语义逐位不变；EventBusStats 公共 record 不改形（并行读面不改既有契约）。

## Out of Scope

- 按会话分桶水位（基数红线）。
- 水位告警联动（读面不裁决）。
- SYNC 模式（无队列无积压——口径不适用）。
