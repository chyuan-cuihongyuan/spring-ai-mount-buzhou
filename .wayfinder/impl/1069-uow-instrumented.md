# 1069 — 事务计量装饰器

**What to build:** InstrumentedUnitOfWork implements UnitOfWork（opt-in 装饰器：三总量+inFlight+失败异常类 Top 有界 8+异常透传）+ 六测。

**Blocked by:** None.

**Status:** done

- [x] InstrumentedUnitOfWork（core/transaction，委托透传含 deleteSession）
- [x] InstrumentedUnitOfWorkTest 六测
- [x] spec 1416 + README 行 + api-surface.md L 段 + 快照再生

## Done

验证：`mvn -pl buzhou-core -am test -Dtest='InstrumentedUnitOfWorkTest'` 6/6 绿。
