---
id: T6045
title: R 会话 R23 尾采样的形状裁决
type: task
status: closed
assignee: zcode-r
blocked-by: []
created: 2026-09-23
---

## Question

错误/慢轨迹这类排障金料怎么不被头采样盲抽丢掉？（spec 4022 / effort #4022 / R23）

## Resolution

**TailSamplingPolicy（core/observability）**：OTel tail-based——整条
轨迹完成后再判：错误必采、慢必采（含等阈值）、其余概率基线且受
预算封顶（金料通道不受预算影响）；Verdict 理由面 + 四计数守恒账。
