---
id: T3166
title: UCB1 选择器的验证裁决
type: task
status: closed
assignee: zcode-p
blocked-by: [T3165]
created: 2026-09-17
---

## Question

Ucb1Selector 合同（未试优先/收敛/不饿死/退化/畸形）怎么钉住？（spec 2032 / effort #2032 / R33）

## Resolution

**七用例一次全绿**（buzhou-core）：三未试臂各恰一试 / 200 轮 good
主导（pulls good>bad）但 bad>0（探索仍在） / good 不幸首抽 0 后 10
轮内必被再探（半径护体不判死刑） / c=0 五连选 best（贪心退化显式） /
空选 null / 均值 0.75 计数 2 / 畸形七型（负 c、null/空臂、重复、
未注册、−0.1、1.1）fail-fast。
