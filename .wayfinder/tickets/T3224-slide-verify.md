---
id: T3224
title: 单调队列滑窗极值的验证裁决
type: task
status: closed
assignee: zcode-p
blocked-by: [T3223]
created: 2026-09-17
---

## Question

SlidingExtremum 合同（窗滑/压制/镜像/畸形）怎么钉住？（spec 2061 / effort #2061 / R62）

## Resolution

**六用例全绿**（两轮断言教训：递增流 size 恒 1——入队即压制是正确
语义非残留，测试期望要按算法模型推非直觉；修后 6/6）：max 六步窗
滑（1/3/3/3/2/5 压制）/ min 镜像（5/2/2/2/1）/ 递增流逐压制 size=1 /
递减流 max 滑出恒新鲜（9/9/5/3）/ 空窗 NaN / 畸形三型（窗 0、NaN、
Inf）fail-fast。
