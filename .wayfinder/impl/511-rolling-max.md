# 511 — HookTiming 滚动 max 读面

**What to build:** RollingMaxCounter（时间桶滚动 max，时钟注入）+ HookTimingAggregator 同发接入 + windowedMax() 快照 + 健康行 rollingMaxMicros——「现在还慢不慢」可答。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] RollingMaxCounter（8×10s 桶环 + 过期重置 + LongSupplier 时钟）
- [x] HookTimingAggregator 接入（record 同发 + windowedMax() 快照，生命周期 max 不动）
- [x] HookTimingHealth 行增 rollingMaxMicros
- [x] 衰减/窗内/翻转/零回归四组用例（注入时钟零等待）
- [x] spec 708 + README 行
- [x] 模块测试绿

## Done

验证：`mvn -pl buzhou-core -am test` 绿。commit 见本轮 `feat(core)` 提交。
