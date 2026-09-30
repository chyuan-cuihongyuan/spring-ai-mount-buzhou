# impl 2414 — X 会话 X12 周期对账（spec 10011 / X10023–X10024 / X12）

纵切片：Wave 2 收口对账——快照批补登 +5（1377 类型）+ 三门批核 + push。

- 验证：`mvn -pl buzhou-spring-boot-starter -am test -Dtest='SpecCoverageTest,XSession10000LedgerAuditTest,ApiSurfaceSnapshotTest'` + 五组件定向测全绿。
