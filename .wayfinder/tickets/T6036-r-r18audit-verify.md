---
id: T6036
title: R 会话 R18 周期对账的验证裁决
type: task
status: closed
assignee: zcode-r
blocked-by: [T6035]
created: 2026-09-23
---

## Question

R18 对账怎么验绿？（spec 4017 / effort #4017 / R18）

## Resolution

**验证通过**：全仓 16 模块离线 `mvn verify` 退出码 0（BUILD
SUCCESS）——快照门（1143 恰等）/覆盖门/对账门（RSession4000 十七
轮四件套）三门全绿；18/50=36% 里程碑。
