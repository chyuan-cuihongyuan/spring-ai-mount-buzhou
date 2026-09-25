# impl 2224 — T 会话 T24 周期对账（spec 6023 / T6247–T6248 / T24）

纵切片：核账封波（快照已于 T18 批补齐 1228）+ 全仓 16 模块
verify 三门 + 台账核账 + 环境确定性清零第二例（lease 水位
min 断言跨毫秒抖动改 ≤first，三连跑绿）。

- 验证：`mvn verify`（根 reactor）BUILD SUCCESS 三门绿；
  `mvn -pl buzhou-spring-boot-starter
  test -Dtest='TSession6000LedgerAuditTest'` 全绿。
