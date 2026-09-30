# impl 2400 — W 会话 W48 W 系 W48 周期对账（spec 9047 / W9095–W9096 / W48）

纵切片：W 系 W48 周期对账——W48 对账轮（spec 9047）：快照批补登 +5 + 覆盖/对账门绿 + 组件 15 测。

- 验证：`mvn -pl buzhou-spring-boot-starter -am test -Dtest='SpecCoverageTest,WSession9000LedgerAuditTest,CronFieldParserTest,PidControllerTest,SimulatedAnnealingTest,TspTwoOptTest,BranchAndBoundTest'` 全绿。
