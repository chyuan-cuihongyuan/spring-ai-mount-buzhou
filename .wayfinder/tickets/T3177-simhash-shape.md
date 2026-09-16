---
id: T3177
title: SimHash 近重复指纹的形状裁决
type: task
status: closed
assignee: zcode-p
blocked-by: []
created: 2026-09-17
---

## Question

文本近重复怎么 O(1) 位比较化？（spec 2038 / effort #2038 / R39）

## Resolution

**Charikar SimHash 纯函数 `SimHashFingerprint`（core/metrics）**：
词元逐位 FNV/splitmix64 散列 + 每 bit 加权投票（±1 求和取符号）→
64 位指纹——相似文本汉明距离小+hammingDistance 对称+isNearDuplicate
阈值判定（≤3 默认）+小词元集平局噪声诚实边界入档（≥16 词元较稳）。
