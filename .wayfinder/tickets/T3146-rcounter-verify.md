---
id: T3146
title: 复制计数器的验证裁决
type: task
status: closed
assignee: zcode-p
blocked-by: [T3145]
created: 2026-09-17
---

## Question

ReplicatedCounter 合同（分量/merge max/三性质/畸形）怎么钉住？（spec 2022 / effort #2022 / R23）

## Resolution

**八用例一次全绿**（buzhou-core）：单 writer 累计 / 多 writer Σ分量 /
负 delta 扣减（PN） / merge 逐分量 max 含新 writer 并入 / 幂等（重复
merge 同值）/ 交换（正反序同分量）/ 三方并发两两 merge 收敛 / 畸形
三型（null writer、null Map、null Counter）fail-fast。
