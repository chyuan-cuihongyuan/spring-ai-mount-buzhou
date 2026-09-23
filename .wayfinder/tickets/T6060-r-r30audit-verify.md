---
id: T6060
title: R 会话 R30 周期对账的验证裁决
type: task
status: closed
assignee: zcode-r
blocked-by: [T6059]
created: 2026-09-23
---

## Question

R30 对账怎么验绿？（spec 4029 / effort #4029 / R30）

## Resolution

**验证通过**：全仓 16 模块离线 `mvn verify` 退出码 0（BUILD
SUCCESS，排除 R18 已入档 flaky）——快照门（1153 恰等）/覆盖门/
对账门（RSession4000 廿九轮四件套）三门全绿；30/50=60% 里程碑。
