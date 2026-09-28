# impl 2236 — T 会话 T36 周期对账（spec 6035 / T6271–T6272 / T36）

纵切片：快照补登 +5（1233→1238）+ api-surface.md/CONTEXT
计数同步 + 全仓 16 模块 verify 三门 + 台账核账。

- 验证：`mvn verify`（根 reactor）BUILD SUCCESS 三门绿；
  `mvn -pl buzhou-spring-boot-starter
  test -Dtest='TSession6000LedgerAuditTest'` 全绿。
