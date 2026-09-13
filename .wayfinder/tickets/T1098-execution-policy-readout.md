---
id: T1098
title: 执行策略汇总读数的裁决
type: task
status: closed
assignee: zcode-g
blocked-by:
created: 2026-09-13
---

## Question
五件套策略无一屏确认面——加 executionPolicy 读数吗？

## Resolution
**用户常设授权 AFK（可推翻）**

决策（G 会话第 49 轮 = effort #748 / spec 748 / impl 649）：`EvalRunner.executionPolicy()` Map 回显当前策略态（预算/重试/超时/记忆化/漂移）——排障「为什么有 [RUN-BUDGET]/[MEMO]」的配置证据面。纯读数。
