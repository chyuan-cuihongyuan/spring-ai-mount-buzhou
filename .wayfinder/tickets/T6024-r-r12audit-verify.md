---
id: T6024
title: R 会话 R12 周期对账的验证裁决
type: task
status: closed
assignee: zcode-r
blocked-by: [T6023]
created: 2026-09-23
---

## Question

R12 对账怎么验绿？（spec 4011 / effort #4011 / R12）

## Resolution

**验证通过**：全仓 16 模块离线 `mvn verify` 退出码 0（BUILD
SUCCESS）——快照门（1138 恰等）/覆盖门/对账门（RSession4000 十一
轮四件套）三门全绿；12/50=24% 里程碑。
