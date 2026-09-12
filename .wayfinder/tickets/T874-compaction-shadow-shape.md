---
id: T874
title: 微压缩影子干跑的形态裁决（纯函数干跑 vs 全链路影子）
type: task
status: closed
assignee: zcode-f
blocked-by:
created: 2026-09-12
---

## Question

Istio mirroring 的思想是「影子流量对照评估不生效」。压缩策略调参（evictRatio/maxAgeTurns）目前只能真压才能看效果。影子干跑应取什么形态？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（F 会话第 13 轮 = effort #600 / spec 612 / impl 465）：

1. **微压缩影子（纯函数干跑）先行**：微压缩本就是确定性纯内存函数（不调 LLM）——干跑 = 调 compact 不应用，天然零成本零风险。LLM 摘要压缩的影子（额外调一次摘要）有真实成本，留雾区。
2. `CompactionShadowEvaluator`：evaluate（单点报告 wouldCompactIds/reclaimedChars）+ sweep（0.25/0.5/0.75/1.0 梯度调参表）+ evaluateAndEmit（`memory.compaction-shadow` 事件，payload 显式 applied=false）。
3. 不写 store、不改历史（零变异由用例钉住）。
