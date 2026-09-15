---
id: T2670
title: 技能漏斗读面的验证门
type: task
status: closed
assignee: zcode-l
blocked-by: T2669
created: 2026-09-15
---

## Question

SkillFunnelStats 怎么验证？（spec 1734 验收/裁决）

## Resolution

SkillFunnelStatsTest：4→2→1 漏斗 0.5/0.5/空双哨兵/无搜索有加载=0 与 1。
