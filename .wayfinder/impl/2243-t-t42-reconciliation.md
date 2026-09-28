# impl 2242 — T 会话 T42 周期对账（spec 6042 / T6285–T6284 / T42）

纵切片：快照补登 +6（1238→1244，含 T43 McsLock 预载）+ api-surface.md/CONTEXT
计数同步 + 全仓 16 模块 verify 三门 + 台账核账 + T37 撞号
平移回填勘误入档。

- 验证：`mvn verify`（根 reactor）BUILD SUCCESS 三门绿；
  `mvn -pl buzhou-spring-boot-starter
  test -Dtest='TSession6000LedgerAuditTest'` 全绿。
