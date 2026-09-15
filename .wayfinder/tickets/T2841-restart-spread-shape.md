---
id: T2841
title: 重启错峰计划的形状裁决
type: task
status: closed
assignee: zcode-o
blocked-by: []
created: 2026-09-16
---

## Question

同批实例重启风暴怎么确定性错峰？（spec 1820 / effort #1820 / R21）

## Resolution

**memberlist/consul 协同重启+AWS jitter 思想纯排程 `RestartSpreadPlan`
（buzhou-resilience）**：delayFor(id, cohortSize, window) 稳定哈希分槽
（同 id 永远同槽，确定性可回放）；cohort 普查带槽碰撞账（鸽笼诚实入账
collisions + collisionRatio -1 哨兵）。零随机数零状态，与 JitterMode（单
调用随机抖动）正交。

