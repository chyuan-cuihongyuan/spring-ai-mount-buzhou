---
id: T6003
title: R 会话 R2 Count-Min 素材计数的形状裁决
type: task
status: closed
assignee: zcode-r
blocked-by: []
created: 2026-09-23
---

## Question

基数爆炸场景任意键点查计数怎么做固定内存近似？（spec 4001 / effort #4001 / R2）

## Resolution

**CountMinSketch（core/metrics）**：Cormode-Muthukrishnan 2005——
d×w 计数矩阵、每行独立散列、estimate 取行最小——非负增量下只高估
不低估（单侧误差）；totalCount 精确守恒；DeterministicHash 行散列
确定性可回放。与 MisraGriesSketch 成对（要名单 vs 要点查）。
