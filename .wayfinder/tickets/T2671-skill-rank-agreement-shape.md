---
id: T2671
title: 技能排序一致性读面的形状裁决
type: task
status: closed
assignee: zcode-l
blocked-by: []
created: 2026-09-15
---

## Question

SkillRankAgreement 的形状怎么裁决？（spec 1735 / effort #1735 / R36）（spec 1735 验收/裁决）

## Resolution

静态纯函数 tau(rankA, rankB) 公共项上 Kendall τ=(C−D)/(C+D) 值域 [−1,1]，公共项<2 哨兵 −1；Agreement(commonItems/concordant/discordant/tau)——sklearn 排序一致性思想。
