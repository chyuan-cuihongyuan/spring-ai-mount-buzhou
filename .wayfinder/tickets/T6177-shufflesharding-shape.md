---
id: T6177
title: S 会话 S39 Shuffle Sharding 洗牌分片的形状裁决
type: task
status: closed
assignee: zcode-s
blocked-by: []
created: 2026-09-25
---

## Question

多租户怎么把坏分片爆炸半径从 n/n 压到 k/n？（spec 5038 /
effort #5038 / S39）

## Resolution

**ShuffleSharding（core/policy）**：AWS shuffle sharding 思想
——每租户种子化随机挑 k 分片（SplitMix64 XOR 指纹 →
Fisher-Yates 取前 k 升序），routesTo 子集路由，overlap 共担
读数（期望 k²/n）；参数越界 fail-fast。
