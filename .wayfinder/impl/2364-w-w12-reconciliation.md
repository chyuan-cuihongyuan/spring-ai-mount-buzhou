# impl 2364 — W 会话 W12 W 系 W12 周期对账（spec 9011 / W9023–W9024 / W12）

纵切片：W 系 W12 周期对账——W12 对账轮（spec 9011）：快照批补登 +5 + 覆盖/对账门绿 + 组件 19 测。

- 验证：`mvn -pl buzhou-spring-boot-starter -am test -Dtest='SpecCoverageTest,WSession9000LedgerAuditTest,BoruvkaMstTest,HeavyLightDecompositionTest,CentroidDecompositionTest,CartesianTreeTest,ScapegoatTreeTest'` 全绿。
