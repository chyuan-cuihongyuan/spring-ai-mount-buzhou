# impl 2151 — S 会话 S1 对账门落位（spec 5000 / T6101–T6102 / S1）

纵切片：SSession5000LedgerAuditTest（starter）——四面互证 +
严格递增断言 + 范围自扩展。

- 验证：`mvn -pl buzhou-spring-boot-starter
  test -Dtest='SSession5000LedgerAuditTest'` 全绿。
