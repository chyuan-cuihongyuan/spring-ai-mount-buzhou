# impl 2212 — T 会话 T12 周期对账（spec 6011 / T6223–T6224 / T12）

纵切片：快照补登 +5（1213→1218）+ api-surface.md/CONTEXT
计数同步 + 全仓 16 模块 verify 三门 + 台账核账。

- 验证：`mvn verify`（根 reactor）BUILD SUCCESS 三门绿；
  `mvn -pl buzhou-spring-boot-starter
  test -Dtest='TSession6000LedgerAuditTest'` 全绿。
