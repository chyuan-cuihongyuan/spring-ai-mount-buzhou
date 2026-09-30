# impl 2402 — W 会话 W50 W 系 W50 收口对账（spec 9049 / W9099–W9100 / W50）

纵切片：W 系 W50 收口对账——W50 收口对账轮（spec 9049）：快照补登 +1 + CONTEXT 同步 + 三门绿 + 台账零缺位 + 封卷声明 + push。

- 验证：`mvn -pl buzhou-spring-boot-starter -am test -Dtest='SpecCoverageTest,WSession9000LedgerAuditTest,ApiSurfaceSnapshotTest'` 全绿。
