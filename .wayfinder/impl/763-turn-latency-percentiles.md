# 763 — 轮次时延分位数读面

**What to build:** TurnLatencyPercentiles record + TurnTimingHook.percentiles（R-7 插值纯函数直测）+ 双轨测试。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] TurnLatencyPercentiles record
- [x] percentiles(sessionId) 窗口排序计算（零值行语义）
- [x] R-7 插值包级纯函数（h=(n−1)·q）
- [x] TurnTimingPercentilesTest（已知值/端点/单样本/越界拒/冒烟）
- [x] spec 1010 + README 行 + API 快照增行

## Done

验证：`mvn -pl buzhou-core test -Dtest='TurnTimingPercentilesTest,TurnTimingHookTest'` 全绿。commit 见本轮 `feat(core)` 提交。
