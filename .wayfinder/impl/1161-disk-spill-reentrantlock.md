# 1161 — DiskSpillStore 锁迁移

**What to build:** spec 1606 高危 #3 落地：store/usage monitor→ReentrantLock + 并发互斥测试。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] store/usage 两方法锁迁移（try/finally）
- [x] DiskSpillStoreConcurrencyTest（同 uri 恰一成功 / 异 uri 全成功）
- [x] spill 模块 168 用例零回归

## Done

验证：`mvn -pl buzhou-spill test` 全绿。
