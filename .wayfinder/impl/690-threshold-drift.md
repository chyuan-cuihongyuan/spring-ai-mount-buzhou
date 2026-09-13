# 690 — gate 阈值漂移读面

**What to build:** EvalGate.thresholdDrift 静态纯函数 + ThresholdDrift record + 测试。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] thresholdDrift
- [x] ThresholdDriftTest（相邻比较/空单条/纯函数）
- [x] spec 938 + README 行（欠账累计 926–938）

## Done

验证：`mvn -pl buzhou-core test -Dtest=ThresholdDriftTest` 全绿。commit 见本轮 `feat(core)` 提交。
