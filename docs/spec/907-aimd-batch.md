# 907 — outbox 投递批量 AIMD 自适应

> 来源：I 会话第 8 轮 = effort #907（[T1265](../../.wayfinder/tickets/T1265-aimd-batch-shape.md) / [T1266](../../.wayfinder/tickets/T1266-aimd-batch-verify.md) / impl 660）。借鉴：TCP 拥塞控制 [AIMD](https://en.wikipedia.org/wiki/Additive_increase/multiplicative_decrease)（RFC 5681 直觉）——加性增、乘性减。

## Problem Statement

`WebhookEventForwarder` 每批固定取 `BATCH=32` 条到期记录：端点故障期固定大批次每轮放大 32 个无效尝试；恢复期又无法上调填满吞吐。批量应随端点健康度自适应。

## 目标

- `WebhookEventForwarder`：
  - opt-in `setAdaptiveBatchEnabled(boolean)`——默认关闭（固定 BATCH 零行为变化）；
  - 口径：自适应初始值 = `BATCH`（32）；单批全部 `DELIVERED` → `+1`（加性增）；批内出现 `RETRYABLE` 或 `FATAL` → `÷2`（乘性减，整除）；夹取 `[ADAPTIVE_MIN=1, ADAPTIVE_MAX=64]`（static final）；
  - `RATE_LIMITED` defer **不算失败**（闸不是故障——spec 718 语义延续，不触发乘减也不触发加增的"全成功"判定——含 defer 的批不计入增减裁决）；
  - 读数 `currentBatchSize()`（测试/运维观测）。
- 既有 `BATCH` 常量与关闭态路径零变化。

## 兼容性

opt-in 增量：默认关闭时投递行为逐位不变。
