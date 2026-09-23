---
id: T6084
title: R 会话 R42 周期对账的验证裁决
type: task
status: closed
assignee: zcode-r
blocked-by: [T6083]
created: 2026-09-24
---

## Question

R42 对账怎么验绿？（spec 4041 / effort #4041 / R42）

## Resolution

**验证通过**：快照再生 +5（reactor classpath）→ 门测试绿；
环境确定性清零第二击（管线测试 latch 无界 await 挂死面根治
——gate 先证 drain 被卡 + latch 限时；UnsubscribedStreamTest
闸释放竞速限时重试）；全仓 16 模块 `mvn verify` BUILD
SUCCESS（退出码 0）；RSession4000LedgerAuditTest 四面互证绿
（spec 4000–4040 卅六轮四件套齐整）。
