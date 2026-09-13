# 771 — taint 信息流控制生命周期计数读面

**What to build:** TaintTrackingHook/TaintWriteGateHook 生命周期计数（TaintMarkStats + GateStats 四分桶守恒）+ 直构骨架测试。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] TaintMarkStats（marksApplied/firstMarks）
- [x] GateStats 四分桶（守恒 checked == trusted + approved + blocked）
- [x] 双 stats() 快照
- [x] TaintLifecycleStatsTest（首标/重复标/四桶/守恒）
- [x] spec 1018 + README 行（嵌套类型不动 API 快照）

## Done

验证：`mvn -pl buzhou-guard test -Dtest='TaintLifecycleStatsTest,TaintWriteGateEndToEndTest'` 全绿。commit 见本轮 `feat(guard)` 提交。
