---
id: T6010
title: R 会话 R5 KS 两样本的验证裁决
type: task
status: closed
assignee: zcode-r
blocked-by: [T6009]
created: 2026-09-23
---

## Question

R5 合同怎么逐一验绿？（spec 4004 / effort #4004 / R5）

## Resolution

**验证通过**：KsTwoSampleTest 五测全绿——全同 10 样本 D=0 p=1；
完全分离 1–10 vs 11–20 D=1 p<0.001；半错位 1–10 vs 6–15 D=0.5
p∈(0.02,0.2)；奇偶交错双半 D<0.1 p>0.5；null/空样本四型 fail-fast。
