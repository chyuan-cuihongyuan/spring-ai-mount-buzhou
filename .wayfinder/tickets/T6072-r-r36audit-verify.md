---
id: T6072
title: R 会话 R36 周期对账的验证裁决
type: task
status: closed
assignee: zcode-r
blocked-by: [T6071]
created: 2026-09-23
---

## Question

R36 对账怎么验绿？（spec 4035 / effort #4035 / R36）

## Resolution

**验证通过**：快照再生 +5（reactor classpath）→ 门测试绿；
环境确定性清零（flush token 入队限时硬化，挂死面根治——
jstack 实证 + 模块 162 测绿复证）；全仓 16 模块 `mvn verify`
BUILD SUCCESS（退出码 0）；RSession4000LedgerAuditTest 四面
互证绿（spec 4000–4034 卅一轮四件套齐整）。
