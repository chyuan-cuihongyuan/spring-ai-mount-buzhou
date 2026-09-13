---
id: T1006
title: 健康加权路由抑制原语的裁决
type: task
status: closed
assignee: zcode-g
blocked-by:
created: 2026-09-12
---

## Question

路由与熔断互不知情——跳闸后流量仍按声明权重撞闸。做健康压权联动吗？压到零还是地板？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（G 会话第 4 轮 = effort #703 / spec 703 / impl 603）：①breaker 加 addTransitionListener 缝（listener 异常隔离）；②RoutingHealthDampener.attach 原语——→OPEN 压权至 floor（默认 1，全跳不黑洞）、→CLOSED 恢复声明权重（dampened 集幂等）、HALF_OPEN 维持地板、未匹配名忽略。零装配原语轮；渐变 ramp 与装配级接线留后续。
