# 503 — 工具执行 per-tool 耗时聚合读面

**What to build:** ToolTimingAggregator（Holder 模式）挂 HookedToolCallback 既有计时点，per-tool count/total/max/failed 进程级聚合 + ToolTimingHealth 健康段（tool-timing，TOP_LIMIT=20 有界）——「哪个工具吃掉最多工具耗时」一屏可读。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] ToolTimingAggregator（LongAdder + CAS max + failed 计数 + stats() 不可变快照 + Holder）
- [x] ToolTimingHealth（BuzhouHealth 恒 UP，per-tool 微秒行 + _truncated）
- [x] HookedToolCallback 计时点镜像（Holder 判空零开销）
- [x] 装配 bean buzhouToolTimingHealth（enable Holder）
- [x] 混合/累计/failed/未装配零回归 + 快照不可变用例
- [x] spec 700 + README 行
- [x] 模块测试绿

## Done

验证：`mvn -pl buzhou-core -am test` 绿。commit 见本轮 `feat(core)` 提交。
