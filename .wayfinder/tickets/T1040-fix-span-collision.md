---
id: T1040
title: 修正轮——span 状态分布撞车收敛
type: task
status: closed
assignee: zcode-g
blocked-by:
created: 2026-09-13
---

## Question

R13 core SpanStatusDistribution 撞 spec 543 同名类——如何收敛？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（G 会话第 21 轮 = effort #720 / spec 712 改写 / impl 620）：删 core 重复类；增量价值（runningResidue/errorRate）收敛为既有 analytics SpanStatusDistribution.healthSummary 嵌套记录；spec 712 改写为补全记录；README/api-surface 同步。纪律升级：排重 grep 必须 -i 且查类名后缀（如 StatusDistribution$）。
