# impl 2394 — W 会话 W42 W 系 W42 周期对账（spec 9041 / W9083–W9084 / W42）

纵切片：W 系 W42 周期对账——W42 对账轮（spec 9041）：快照批补登 +5 + 覆盖/对账门绿 + 组件 15 测。

- 验证：`mvn -pl buzhou-spring-boot-starter -am test -Dtest='SpecCoverageTest,WSession9000LedgerAuditTest,MurmurHash3Test,DiffieHellmanExchangeTest,XorShift64Test,SplitMix64Test,BitonicSorterTest'` 全绿。
