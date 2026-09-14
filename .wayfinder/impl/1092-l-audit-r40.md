# 1092 — L 会话阶段对账轮（R40）

**What to build:** LSessionLedgerAuditTest 对账测试（四面互证+范围自扩展）+ 票号/impl/spec 重编号修复批次。

**Blocked by:** None.

**Status:** done

- [x] 对账测试四测（票对/impl 窗/README 行/号连续）
- [x] 修复：18 票 -2 重编号+id 字段+spec 1439→1438+impl 三片置换+全仓引用单遍替换
- [x] spec 1440 + README 行（测试域——快照面不变）

## Done

验证：`mvn -pl buzhou-spring-boot-starter -am test -Dtest='LSessionLedgerAuditTest,SpecCoverageTest,ApiSurfaceSnapshotTest'` 全绿。
