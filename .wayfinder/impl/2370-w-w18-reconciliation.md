# impl 2370 — W 会话 W18 W 系 W18 周期对账（spec 9017 / W9035–W9036 / W18）

纵切片：W 系 W18 周期对账——W18 对账轮（spec 9017）：快照批补登 +5 + 覆盖/对账门绿 + 组件 18 测。

- 验证：`mvn -pl buzhou-spring-boot-starter -am test -Dtest='SpecCoverageTest,WSession9000LedgerAuditTest,LzwCodecTest,BurrowsWheelerTransformTest,MoveToFrontTransformTest,Lz77CodecTest,AnsCodecTest'` 全绿。
