---
id: T1265
title: outbox 投递批量 AIMD 自适应的形态裁决
type: task
status: closed
assignee: zcode-i
blocked-by:
created: 2026-09-13
---

## Question

I 会话第 8 轮：WebhookEventForwarder 投递批量 BATCH=32 固定——AIMD 自适应（TCP 拥塞控制思想）是否有缺口？形态如何裁决？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（I 会话第 8 轮 = effort #907 / spec 907 / impl 660）：缺口成立——端点故障期固定 32 批次放大无效重试（每批 32 个失败样本），恢复期又压不满吞吐。落点 `WebhookEventForwarder`：**opt-in** `setAdaptiveBatchEnabled(true)`（默认 false=固定 32 零行为变化）。口径：初始 = BATCH(32)；一批内**全部 DELIVERED** → 加性增 +1（AIMD 加性增）；批内出现 RETRYABLE/FATAL → 乘性减 ÷2（指数退避直觉）；夹取 [1, 64]（ADAPTIVE_MIN/ADAPTIVE_MAX static final）；限速 defer 不算失败（defer 是闸不是故障——spec 718 语义延续）；读数 `currentBatchSize()`。测试经 HttpServer Collector.status 控制outcome 驱动增/减/夹取三态。
