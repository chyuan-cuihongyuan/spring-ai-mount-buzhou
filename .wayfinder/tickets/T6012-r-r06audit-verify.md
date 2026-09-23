---
id: T6012
title: R 会话 R6 周期对账的验证裁决
type: task
status: closed
assignee: zcode-r
blocked-by: [T6011]
created: 2026-09-23
---

## Question

R6 对账怎么验绿？（spec 4005 / effort #4005 / R6）

## Resolution

**验证通过**：全仓 16 模块离线 `mvn verify` 退出码 0（BUILD
SUCCESS）——快照门（1133 类型恰等）/覆盖门（spec↔README 双向）/
对账门（RSession4000 五轮四件套）三门全绿；6/50=12% 里程碑。
