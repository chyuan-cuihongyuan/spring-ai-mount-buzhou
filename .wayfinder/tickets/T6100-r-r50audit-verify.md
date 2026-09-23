---
id: T6100
title: R 会话 R50 收口对账的验证裁决
type: task
status: closed
assignee: zcode-r
blocked-by: [T6099]
created: 2026-09-24
---

## Question

R50 收口怎么验绿？（spec 4049 / effort #4049 / R50）

## Resolution

**验证通过**：快照再生 +1（reactor classpath）→ 门测试绿；
全仓 16 模块 `mvn verify` BUILD SUCCESS（退出码 0；R48 协议
口径排除两入档 flaky）；台账全量核账绿（spec 4000–4049
五十轮四件套齐整、号段严格递增）。
