# 1168 — 泄漏疑似聚合接线

**What to build:** LeakSuspectHolder 复合 listener + 装配处替换。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] LeakSuspectHolder（compositeWith/report/install）
- [x] BuzhouCoreAutoConfiguration detector 构造接复合
- [x] LeakSuspectHolderTest 两断言 + 既有 4 用例零回归

## Done

验证：`mvn -pl buzhou-core test -Dtest=LeakSuspect*` 全绿。
