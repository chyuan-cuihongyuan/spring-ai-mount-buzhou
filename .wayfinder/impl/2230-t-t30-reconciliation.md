# impl 2230 — T 会话 T30 周期对账（spec 6029 / T6259–T6260 / T30）

纵切片：快照补登 +5（1228→1233）+ api-surface.md/CONTEXT
计数同步 + 全仓 16 模块 verify 三门 + 台账核账。

- 验证：`mvn verify`（根 reactor）BUILD SUCCESS 三门绿；
  `mvn -pl buzhou-spring-boot-starter
  test -Dtest='TSession6000LedgerAuditTest'` 全绿。
