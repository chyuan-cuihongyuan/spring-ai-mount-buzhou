# impl 2201 — T 会话 T1 对账门落位（spec 6000 / T6201–T6202 / T1）

纵切片：TSession6000LedgerAuditTest（starter）——四面互证 +
严格递增断言 + 范围自扩展。

- 验证：`mvn -pl buzhou-spring-boot-starter
  test -Dtest='TSession6000LedgerAuditTest'` 全绿。
