---
id: T1540
title: 围栏裁决分布读面的验证裁决
type: task
status: closed
assignee: zcode-j
blocked-by: T1539
created: 2026-09-14
---

## Question

J 会话第 43 轮：裁决分布读面如何验证？

## Resolution

**用户常设授权 AFK（可推翻）**

验证裁决（FenceVerdictStatsTest，复用 spec 303 判定矩阵场景）：首见建基线 CONTINUE；连续 CONTINUE；跳号 GAP；重复 DUPLICATE；同纪元倒退 RESET；显式新纪元 RESET；旧纪元迟到 STALE；守恒 Σ五桶 == observe 调用数；快照不可变；fresh 零值。定向 `mvn -pl buzhou-core test -Dtest='FenceVerdictStatsTest'` 绿 + 既有围栏回归绿。
