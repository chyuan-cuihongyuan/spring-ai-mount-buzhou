# 524 — 死信重放审计事件

**What to build:** replayDeadLetters 审计增强——buzhou.webhook.dead-replayed 指标（delta=条数，零重放不发）+ replayCount/replayedCount 累计 + 结构化审计日志。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] 指标 + 累计计数 + 审计日志
- [x] 3 条重放/零重放/零回归用例
- [x] spec 721 + README 行
- [x] 模块测试绿

## Done

验证：`mvn -pl buzhou-core -am test` 绿。commit 见本轮 `feat(core)` 提交。
