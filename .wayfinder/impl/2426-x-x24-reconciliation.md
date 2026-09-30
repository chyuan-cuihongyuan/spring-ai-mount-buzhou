# impl 2426 — X 会话 X24 周期对账（spec 10023 / X10047–X10048 / X24）

纵切片：Wave 4 收口对账——快照批补登 +5（1387 类型）+ 三门批核 + push。

- 验证：`mvn -pl buzhou-spring-boot-starter -am test -Dtest='SpecCoverageTest,XSession10000LedgerAuditTest,ApiSurfaceSnapshotTest'` + 五组件定向测全绿。
