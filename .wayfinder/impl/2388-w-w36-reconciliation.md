# impl 2388 — W 会话 W36 W 系 W36 周期对账（spec 9035 / W9071–W9072 / W36）

纵切片：W 系 W36 周期对账——W36 对账轮（spec 9035）：快照批补登 +5 + 覆盖/对账门绿 + 组件 14 测。

- 验证：`mvn -pl buzhou-spring-boot-starter -am test -Dtest='SpecCoverageTest,WSession9000LedgerAuditTest,MillerRabinPrimalityTest,KaratsubaMultiplicationTest,FastInverseSqrtTest,GeometricMedianTest,BezierCurveTest'` 全绿。
