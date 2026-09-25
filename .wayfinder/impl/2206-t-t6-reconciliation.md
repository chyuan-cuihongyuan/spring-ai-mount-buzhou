# impl 2206 — T 会话 T6 周期对账（spec 6005 / T6211–T6212 / T6）

纵切片：快照补登 +4（1209→1213）+ api-surface.md/CONTEXT 计数
同步 + 全仓 16 模块 verify 三门 + TSession6000LedgerAuditTest
核账。

- 验证：`mvn verify`（根 reactor）BUILD SUCCESS 三门绿；
  `mvn -pl buzhou-spring-boot-starter
  test -Dtest='TSession6000LedgerAuditTest'` 全绿。
