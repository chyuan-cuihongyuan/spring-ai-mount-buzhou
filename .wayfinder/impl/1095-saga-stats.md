# 1095 — Saga 运行静态读数

**What to build:** CompensatingBatch 增量（sagaStats 漏斗四计数+守恒+断点步名+reset）+ 四测。

**Blocked by:** None.

**Status:** done

- [x] CompensatingBatch 静态读数（run 四点+unwind 失败分支埋点）
- [x] CompensatingBatchSagaStatsTest 四测
- [x] spec 1443 + README 行（既有类静态字段——快照面不变）

## Done

验证：`mvn -pl buzhou-core -am test -Dtest='CompensatingBatchSagaStatsTest,CompensatingBatchTest'` 8/8 绿。
