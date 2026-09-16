---
id: T3123
title: P 会话 R12 对账轮的形状裁决
type: task
status: closed
assignee: zcode-p
blocked-by: []
created: 2026-09-17
---

## Question

Wave 2 五新类型的快照门与积压推送怎么收口？（spec 2011 / effort #2011 / R12）

## Resolution

**R6k 第二例四件套**：regenerateSnapshot 快照 1006→1011（+5：LWW/
频率素描/重试主机排除/FlagEvaluator/DecisionCache）+ api-surface.md
五行 + CONTEXT 905→910 + 全仓 verify 三门绿 + push 补推（R11 积压
一并）。push 波动处置定式：内容轮不阻塞、对账轮集中重试。
