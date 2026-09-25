# impl 2218 — T 会话 T18 周期对账（spec 6017 / T6235–T6236 / T18）

纵切片：快照补登 +10（1218→1228，含 Wave 4 预载×5）+ api-surface.md/CONTEXT
计数同步 + 全仓 16 模块 verify 三门 + 台账核账 + 环境确定性
清零（leaseExpiresNaturally 1ms TTL 相邻语句间隙假红改轮询
等自然到期——单跑复绿入档）。

- 验证：`mvn verify`（根 reactor）BUILD SUCCESS 三门绿；
  `mvn -pl buzhou-spring-boot-starter
  test -Dtest='TSession6000LedgerAuditTest'` 全绿。
