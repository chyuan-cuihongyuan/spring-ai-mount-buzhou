# 1159 — 虚拟线程 pinning 审计 + 金丝雀热路径修复

**What to build:** 全仓 synchronized-IO 审计入档 + CanaryToolCallback.route 三段式修复 + 并行断言测试。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] Explore 子代理只读审计（17 组：高危 6 / 中危 11 / 低危 ~85 类）
- [x] CanaryToolCallback.route 三段式（决策锁内/执行锁外/计数锁内）
- [x] CanaryConcurrencyTest 双 latch 并行断言 + 既有 7 用例零回归
- [x] spec 1606 审计留痕 + 后续修复排队

## Done

验证：`mvn -pl buzhou-core test -Dtest=Canary*` 全绿（8 用例）。
