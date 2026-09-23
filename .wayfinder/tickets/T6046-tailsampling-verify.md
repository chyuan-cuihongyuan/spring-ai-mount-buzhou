---
id: T6046
title: R 会话 R23 尾采样的验证裁决
type: task
status: closed
assignee: zcode-r
blocked-by: [T6045]
created: 2026-09-23
---

## Question

R23 合同怎么逐一验绿？（spec 4022 / effort #4022 / R23）

## Resolution

**验证通过**：TailSamplingPolicyTest 五测全绿——概率 0+预算 0 也不
拦错误；999 丢/1000 恰阈采含等/5000 采；p=1 全采、p=0 全丢、
p=0.5 千次 400–600 种子确定 + 守恒账；预算 2 恰采两后 budget 丢 +
错误/慢通道不受预算影响；畸形六型 fail-fast。首版 long 断言字面量
笔误已修（isBetween 400L）。
