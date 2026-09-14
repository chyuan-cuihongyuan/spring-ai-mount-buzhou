# 1160 — RollingJsonlWriter 锁迁移

**What to build:** spec 1606 审计高危 #2 落地：monitor→ReentrantLock + 并发完整性测试。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] appendLine/close/bytesWritten 三方法锁迁移（try/finally）
- [x] Javadoc 线程安全段更新（pinning 语义说明）
- [x] RollingJsonlWriterConcurrencyTest 并发零撕裂
- [x] 既有 RollingJsonl* 测试零回归

## Done

验证：`mvn -pl buzhou-core test -Dtest=RollingJsonl*` 全绿。
