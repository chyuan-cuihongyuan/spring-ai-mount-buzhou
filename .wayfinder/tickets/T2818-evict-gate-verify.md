---
id: T2818
title: 驱逐信号阈值门的落地验证
type: task
status: closed
assignee: zcode-o
blocked-by: [T2817]
created: 2026-09-16
---

## Question

三态裁决与普查在四情形/空表/畸形下正确吗？（spec 1808 / effort #1808 / R9）

## Resolution

**EvictionThresholdGateTest 4 用例全绿**（mvn -pl buzhou-spill test
-Dtest=EvictionThresholdGateTest）：三态四情形（below/pending/宽限满/硬即逐）；
普查计数+evictRatio+null 空表；空普查哨兵；阈值倒挂/NaN/负毫秒/NaN 信号
fail-fast。

