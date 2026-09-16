---
id: T3109
title: φ 累积故障嫌疑度检测器的形状裁决
type: task
status: closed
assignee: zcode-p
blocked-by: []
created: 2026-09-17
---

## Question

心跳故障判定怎么从二值跳变升级为连续嫌疑度？（spec 2004 / effort #2004 / R5）

## Resolution

**Hayashibara/Finagle 线程安全 φ 检测器 `PhiAccrualFailureDetector`
（core/concurrent）**：间隔滑动窗（默认 1000，回拨 fail-fast）正态模型
（mean/std + std 下界 100ms 防规律退化）+ 右尾概率 erf 近似 +
φ=−log₁₀(p) 钳 [0,12] + 样本 <2 恒 0 + sampleCount/meanIntervalMillis
读数。时间调用方传入——确定性可回放。
