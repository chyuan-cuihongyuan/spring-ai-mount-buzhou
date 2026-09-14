# 1105 — 计时聚合器双子实例清零面（M 系 R3）

**What to build:** HookTimingAggregator / ToolTimingAggregator 各补公开 reset()（清空 timings，幂等，不碰 Holder 开关）。

**Blocked by:** T2255 / T2256（同轮 shape+verify）。

**Status:** done

- [x] 双聚合器 reset()（Prometheus counter reset 语义，Javadoc 双用途注明）
- [x] stats()/windowedMax() 同步清零断言 + 幂等 + Holder 开关不动断言
- [x] 既有测试零回归（11 用例绿）

## Done

验证：定向 11 用例绿。commit 见本轮 feat 提交。
