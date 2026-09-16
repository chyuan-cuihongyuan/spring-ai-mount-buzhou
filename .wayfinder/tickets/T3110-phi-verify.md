---
id: T3110
title: φ 累积故障嫌疑度检测器的验证裁决
type: task
status: closed
assignee: zcode-p
blocked-by: [T3109]
created: 2026-09-17
---

## Question:

PhiAccrualFailureDetector 合同（低/高嫌疑分档/单调/上下界/自适应/畸形）怎么钉住？（spec 2004 / effort #2004 / R5）

## Resolution

**八用例一次全绿**（buzhou-core）：规律心跳 10.5s 处 φ<2 / 15s 处
φ>4 / φ 随沉默单调增 / 远超期钳上界恰 12 / 从未心跳与单样本恒 0 /
std 下界（规律心跳均值处 φ≈0.301=−log10(0.5)）/ 窗滑动重学节奏
（1000ms→100ms 后 5 均值未到 φ>3）/ 畸形三型（窗 1、std 0、回拨）
fail-fast。
