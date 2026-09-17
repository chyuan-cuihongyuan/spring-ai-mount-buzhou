---
id: T5021
title: Q 会话 R11 MinHash 素描的形状裁决
type: task
status: closed
assignee: zcode-q
blocked-by: []
created: 2026-09-18
---

## Question

近重复判定怎么避开 O(n²) 全量两两？（spec 3010 / effort #3010 / R11）

## Resolution

**MinHashSketch（core/metrics）**：k 路最小哈希签名，相等位占比
= Jaccard 无偏估计；确定性派生（DeterministicHash 基散列+Weyl 掺
index+splitmix64 终结器——同集合同签名可审计）；重复 offer 幂等；
空集合 NaN/0 诚实；粗筛+精复两层口径替代全量精确。
