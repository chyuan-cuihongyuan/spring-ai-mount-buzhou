---
id: T1539
title: 围栏裁决分布读面的形态裁决
type: task
status: closed
assignee: zcode-j
blocked-by:
created: 2026-09-14
---

## Question

J 会话第 43 轮：围栏裁决分布读面（Kafka epoch/consumer-lag 思想续 spec 303）在本仓是否有缺口？形态如何裁决？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（J 会话第 43 轮 = effort #1043 / spec 1043 / impl 795）：缺口成立——SequenceFence.observe 五态判定（CONTINUE/GAP/DUPLICATE/RESET/STALE）逐次返回但零聚合：GAP 频度（上游丢事件）、DUPLICATE 频度（重投风暴）、RESET 频度（发送方重启/倒退异常）无水位——订阅流健康度只可逐条翻裁决。落点 core/webhook：实例级五桶 verdict 计数（固定键有界）+ 嵌套 record `FenceVerdictStats(byVerdict, total)` + `stats()` 快照；observe 重构为 judge 纯函数 + 计数包装——判定矩阵与基线语义逐位不变。
