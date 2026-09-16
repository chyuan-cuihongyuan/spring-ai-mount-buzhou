---
id: T3103
title: HLL 基数素描的形状裁决
type: task
status: closed
assignee: zcode-p
blocked-by: []
created: 2026-09-17
---

## Question

高基数 distinct 计数怎么在定容内存 + 可合并语义下账户化？（spec 2001 / effort #2001 / R2）

## Resolution

**Redis HLL / Flajolet 线程安全素描 `HllCardinalitySketch`（core/metrics）**：
precision ∈ [4,16] fail-fast + FNV-1a 64/splitmix64 确定性散列（前 b 位选
寄存器/尾 rank 取 max，幂等）+ 调和平均估计（小值域线性计数修正）+
merge 逐位 max 并集语义 + relativeErrorBound=1.04/√m 读数 +
offeredCount 重复率对账面。与 SessionBloomFilter 互补：布伦答存在性，
HLL 答基数。
