# impl 2358 — W 会话 W6 W 系 W6 周期对账（spec 9005 / W9011–W9012 / W6）

纵切片：W 系 W6 周期对账——W6 对账轮（spec 9005）：快照批补登 +4 + 三门绿 + 台账核账 9000–9004 零缺位。

- 验证：`mvn -pl buzhou-spring-boot-starter -am test -Dtest='ApiSurfaceSnapshotTest,SpecCoverageTest,WSession9000LedgerAuditTest,HopcroftKarpMatcherTest,EdmondsKarpMaxFlowTest,StoerWagnerMinCutTest,BronKerboschCliquesTest'` 全绿。
