# impl 2382 — W 会话 W30 W 系 W30 周期对账（spec 9029 / W9059–W9060 / W30）

纵切片：W 系 W30 周期对账——W30 对账轮（spec 9029）：快照批补登 +5 + 覆盖/对账门绿 + 组件 15 测。

- 验证：`mvn -pl buzhou-spring-boot-starter -am test -Dtest='SpecCoverageTest,WSession9000LedgerAuditTest,Exp3BanditTest,GradientBanditTest,MetropolisHastingsTest,PermutationTestFileTest,JackknifeEstimatorTest'` 全绿。
