---
id: T1285
title: 健康评分端点装配的形态裁决
type: task
status: closed
assignee: zcode-i
blocked-by:
created: 2026-09-14
---

## Question

I 会话第 18 轮：spec 905 明确留位「端点接线归装配轮」——BuzhouHealthScore 接入 `/actuator/buzhou` 快照是否有缺口？装配形态如何裁决？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（I 会话第 18 轮 = effort #917 / spec 917 / impl 670）：留位到期，缺口成立——评分只有编程读面，dashboard/运维「一屏一格」的原始诉求未兑现。落点 `BuzhouHealthEndpoint.buzhouSnapshot()`：返回 Map 增加 `"score"` 段（`BuzhouHealthScore.compute(contributors)` 投影：score/tier/upCount/downCount/unknownCount/downMechanisms——ScoreReport 本身有界纪律成立，投影取全字段）。compute 异常隔离（try/catch 降级 scoreError——与既有 safeDetails 同风格）。既有断言为 containsKey("mechanisms") 不受影响（纯增量键）。
