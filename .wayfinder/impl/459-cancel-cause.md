# 459 — 取消原因枚举传播

**What to build:** `CancelCause` 枚举 + `AgentSession.cancel(CancelMode, CancelCause)`（default 委托）+ DefaultAgentSession 覆写（事件 payload + `buzhou.session.cancelled` 指标 tag cause）+ 停机排水传 SHUTDOWN_DRAIN。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] CancelCause 闭集（USER/SHUTDOWN_DRAIN/DEADLINE/LEASE_LOST/RUNAWAY）
- [x] 接口 default + DefaultAgentSession 覆写 + 指标
- [x] cancelQuietly 停机接线
- [x] CancelCauseTest 3/3 + core 全模块 1749/1749 零回归
- [x] spec 606 + README 行

## Done

验证：`mvn -pl buzhou-core test` 绿。commit 见本轮 `feat(core)` 提交。
