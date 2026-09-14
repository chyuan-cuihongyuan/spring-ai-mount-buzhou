# 1101 — 轮次采样漏斗读面

**What to build:** TurnSamplerHook 静态漏斗增量（六计数：turnsSeen/written/rate/short/empty/writeFailures）+ 三测。

**Blocked by:** None.

**Status:** done

- [x] afterTurn 埋点（ratePercent=0 早退不入账、fail-soft 写失败分桶）
- [x] TurnSamplerHookStatsTest 三测
- [x] spec 1446 + README 行（既有类静态字段——快照面不变）

## Done

验证：`mvn -pl buzhou-core -am test -Dtest='TurnSamplerHookStatsTest'` 3/3 绿。
