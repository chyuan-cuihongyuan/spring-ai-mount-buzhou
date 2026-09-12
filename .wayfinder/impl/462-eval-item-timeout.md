# 462 — 评估项级超时预算

**What to build:** EvalRunner 项级超时包装（虚拟线程 + future.get + shutdownNow 中断传播），超时收敛 error + 指标；默认 null 零变化。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] setPerItemTimeout（校验）+ runItemWithTimeout 包装（串行/并行两路）
- [x] 3 用例绿（挂死收敛/并行/校验）
- [x] spec 609 + README 行

## Done

验证：`mvn -pl buzhou-core test -Dtest=EvalItemTimeoutTest` 绿（3/3）。commit 见本轮 `feat(core)` 提交。
