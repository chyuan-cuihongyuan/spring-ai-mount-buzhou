# impl 2353 — W 会话 W1 9000 系对账门落位（spec 9000 / W9001–W9002 / W1）

纵切片：WSession9000LedgerAuditTest（starter）——spec 9000–9049 号段
扫驱动四面互证（票对 9001+2(N−9000) / impl 2353+(N−9000) /
README 覆盖 / 起点严格递增），公式族第十应用。

- 验证：`mvn -pl buzhou-spring-boot-starter test -Dtest='WSession9000LedgerAuditTest'` 四测全绿（spec 9000 自证）。
