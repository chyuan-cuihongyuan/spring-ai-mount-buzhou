---
id: T3174
title: 迟滞水位门的验证裁决
type: task
status: closed
assignee: zcode-p
blocked-by: [T3173]
created: 2026-09-17
---

## Question

HysteresisWatermark 合同（高停低续/保持区/钳制/畸形）怎么钉住？（spec 2036 / effort #2036 / R37）

## Resolution

**七用例一次全绿**（buzhou-core）：恰 HIGH（100）不停超 1（101）停 /
泄到 110 保持停、钳 0 恢复（停+续恰两翻） / 保持区全程恰两次翻转
（160→80 停态延续→5 恢复——无抖动） / 恰 LOW=10 恢复（≤ 语义） /
超泄钳 0 / 累计 400 追踪 / 畸形五型（负 LOW、重合 100=100、倒置、
负 write、负 drain）fail-fast。
