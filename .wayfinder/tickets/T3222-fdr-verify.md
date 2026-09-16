---
id: T3222
title: BH-FDR 校正的验证裁决
type: task
status: closed
assignee: zcode-p
blocked-by: [T3221]
created: 2026-09-17
---

## Question

FalseDiscoveryRate 合同（截止序/不救援/功效对比/退化/畸形）怎么钉住？（spec 2060 / effort #2060 / R61）

## Resolution

**六用例全绿**（首跑 1 红教训：Holm 对照断言的手算错——0.02>0.0167
即止只 1 显著非 2，修后 6/6）：截止序 m=5 恰两显 / 孤立 0.01 在大 p
海中仅自身显著 / BH 四全显 vs Holm 仅 1（阈值序列手算对比） / 全大
p 零 / m=1 退化 p≤q / 畸形六型（null、空、q 0、q 1、p −0.1、p 1.1）
fail-fast。
