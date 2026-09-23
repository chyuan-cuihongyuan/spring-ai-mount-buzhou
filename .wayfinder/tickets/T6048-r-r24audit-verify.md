---
id: T6048
title: R 会话 R24 周期对账的验证裁决
type: task
status: closed
assignee: zcode-r
blocked-by: [T6047]
created: 2026-09-23
---

## Question

R24 对账怎么验绿？（spec 4023 / effort #4023 / R24）

## Resolution

**验证通过**：全仓 16 模块离线 `mvn verify` 退出码 0（BUILD
SUCCESS，排除 R18 已入档的 ShadowMirror flaky）——快照门（1148
恰等）/覆盖门/对账门（RSession4000 廿三轮四件套）三门全绿；
24/50=48% 里程碑。
