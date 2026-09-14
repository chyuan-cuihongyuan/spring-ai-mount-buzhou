# 1053 — 跨会话轮次并发水位观察者 + guard-block 收口修复

**What to build:** TurnConcurrencyTracker implements SessionObserver（started/okFinished/failed 三总量 + active/peakActive 水位 + stats()/resetForTest()）+ DefaultAgentSession 两处 guard-block 路径补派终结回调（非流式 onTurnEnd / 流式 onTurnError）+ 单测五测 + E2E 三测。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] TurnConcurrencyTracker（core/session，原子记账无锁，Snapshot 嵌套 record）
- [x] doChatTurn Block 路径补 onTurnEnd + recordTurnDuration(ok)；流式 Block 路径补 onTurnError
- [x] TurnConcurrencyTrackerTest（守恒/峰值单调/双终结防御/reset/并发压测）
- [x] GuardBlockObserverClosureTest（非流式/流式/守恒 E2E）
- [x] spec 1400 + README 行 + api-surface.md L 段 + 快照再生

## Done

验证：`mvn -pl buzhou-core -am test -Dtest='TurnConcurrencyTrackerTest,GuardBlockObserverClosureTest'` 8/8 绿；全模块回归 2357 测 1 flaky（HookEndToEndTest，重跑绿，零交集）。
