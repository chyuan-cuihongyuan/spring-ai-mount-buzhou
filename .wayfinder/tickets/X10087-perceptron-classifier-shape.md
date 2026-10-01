---
id: X10087
title: X 会话 44 PerceptronClassifier 感知机 的形状裁决
type: task
status: closed
assignee: zcode-x
blocked-by: []
created: 2026-10-01
---

## Question

形状怎么定？

## Resolution

PerceptronClassifier（core/metrics）：fit(features,labels,lr,epochs) 错分驱动在线更新 w+=lr(y−ŷ)x+b 同式+predict 符号面；标签 {0,1} 口径；线性可分域承诺（不可分不收敛明示）；null/标签越界/非正 lr/epochs fail-fast。
