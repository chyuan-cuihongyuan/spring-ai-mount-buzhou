# 758 — 工具在飞并发水位读面

**What to build:** ToolInFlight（enter/Lease 恰一次 close + 每工具 current/peak/total + 全局双水位 + snapshot/reset）+ HookedToolCallback try/finally 接线 + 并发水位测试。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] ToolInFlight + 嵌套 Lease/PerTool/Snapshot
- [x] 峰值 CAS 只增不降 + close 恰一次
- [x] HookedToolCallback 接线（timer 同点 try/finally）
- [x] ToolInFlightTest（增减/峰值/跨工具/恰一次/不可变/reset）
- [x] spec 1005 + README 行 + API 快照与 api-surface.md 增行

## Done

验证：`mvn -pl buzhou-core test -Dtest='ToolInFlightTest,ToolDurationTimerTest'` 全绿。commit 见本轮 `feat(core)` 提交。
