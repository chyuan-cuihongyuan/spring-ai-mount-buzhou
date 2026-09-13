# 1043 — 围栏裁决分布读面

> 来源：J 会话第 43 轮 = effort #1043（[T1539](../../.wayfinder/tickets/T1539-fence-verdict-stats-shape.md) / [T1540](../../.wayfinder/tickets/T1540-fence-verdict-stats-verify.md) / impl 795）。与 R30/R38 同族：判定/操作的跨调用聚合水位（Kafka consumer-lag/epoch 思想续 spec 303）。

## Problem Statement

SequenceFence（spec 159/303，投递序号围栏）observe 逐次返回五态裁决（CONTINUE/GAP/DUPLICATE/RESET/STALE），但裁决分布零聚合：GAP 频度（上游丢事件）、DUPLICATE 频度（重投风暴）、RESET 频度（发送方重启/纪元切换异常）无水位——订阅流健康度只可逐条翻裁决。

## 目标

- `SequenceFence` 增量（core/webhook，实例级）：五桶 verdict 计数（固定键：CONTINUE/GAP/DUPLICATE/RESET/STALE，有界）+ `stats()` 快照。
- 观察逻辑重构：判定矩阵原样抽为纯函数，observe 包装计数与返回——**判定矩阵与基线语义逐位不变**。
- 嵌套 record `FenceVerdictStats(Map<String, Long> byVerdict)`（恒含五键）+ `total()` 派生。

## 兼容性

纯增量读面；无新配置项。

## Out of Scope

- 按订阅流分桶（订阅流数可能无界——总量桶即可）。
- GAP 区间累计对账（lastGap 已有单点面）。
