# impl 2001 — Q 会话 R1 对账门落位（spec 3000 / T5001–T5002 / R1）

纵切片：QSession3000LedgerAuditTest 四面互证 + 3000 系总图开图 +
MAP.md 登记 + 号段声明（efforts #3000–#3149 / T5001–T5300 / impl
2001–2150，fetch 已通 + 本地全档双查空闲）。

- 验证：`mvn -pl buzhou-spring-boot-starter -am test -Dtest='QSession3000LedgerAuditTest'` 全绿。
