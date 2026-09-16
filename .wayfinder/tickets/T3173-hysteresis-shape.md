---
id: T3173
title: 迟滞水位门的形状裁决
type: task
status: closed
assignee: zcode-p
blocked-by: []
created: 2026-09-17
---

## Question

水位背压的单阈值抖动怎么根治？（spec 2036 / effort #2036 / R37）

## Resolution

**Netty write watermark 迟滞门 `HysteresisWatermark`
（core/backpressure，纯记账）**：超 HIGH 停写+泄到 ≤ LOW 才恢复——
两阈值间保持区状态延续不翻转（迟滞防抖核心）+toggleCount 翻转计数
（两阈值过近诊断信号）+high>low fail-fast（重合即退化单阈值抖动门
不合法）+泄出钳 0。
