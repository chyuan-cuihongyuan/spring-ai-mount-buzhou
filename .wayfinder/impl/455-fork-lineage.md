# 455 — 会话 fork 谱系

**What to build:** fork / forkFromTurn 两入口向子会话写谱系 state（`buzhou.fork.source` → 源会话 id）；`session.forked` 事件携带 copiedMessages / copiedSummary（/ upToTurn）；导出天然携带。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] 两入口谱系写入（普通 fork + 时间旅行 fork）
- [x] 事件 payload 增强（copy 计数 + summary 布尔 + upToTurn）
- [x] SessionForkLineageTest 3 用例（state 写入/事件计数/导出携带）全绿
- [x] SessionForkEndToEndTest 零回归（State 不复制语义不受影响）
- [x] spec 602 + README 行

## Done

验证：`mvn -pl buzhou-core,buzhou-resilience -am test` 绿（core 1745/1745 含新 3 用例）。commit 见本轮 `feat(core)` 提交。
