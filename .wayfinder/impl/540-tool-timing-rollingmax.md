# 540 — 工具侧滚动 max 同构扩散

**What to build:** ToolTimingAggregator windowedMax + ToolTimingHealth rollingMaxMicros 行（spec 708 同构）。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] windowedMax 接入 + 健康行
- [x] 用例 + 零回归
- [x] spec 738 + README 行
- [x] 模块测试绿

## Done

验证：`mvn -pl buzhou-core -am test` 绿。commit 见本轮 `feat(core)` 提交。
