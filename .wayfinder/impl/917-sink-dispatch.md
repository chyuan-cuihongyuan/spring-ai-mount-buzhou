# 917 — BaseSpanRecorder sink 分发补测（R15）

**What to build:** BaseSpanRecorderSinkDispatchTest（5 用例：span/event 分发到达、throwing sink 异常隔离、PendingSnapshot 跳过、sinks 空）。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] BaseSpanRecorderSinkDispatchTest（5 用例）
- [x] spec 1214 + README 行
- [x] 验证：123 用例全绿；BaseSpanRecorder 85%

## Done

验证：observability 123 用例全绿；sink 分发骨架 85%。commit 见本轮 `test(observability)` 提交。
