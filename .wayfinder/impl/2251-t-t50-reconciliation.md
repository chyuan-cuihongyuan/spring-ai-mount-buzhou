# impl 2251 — T 会话 T50 收口对账（spec 6050 / T6301–T6302 / T50）

纵切片：快照补登 +1（1248→1249）+ 对账门范围勘误扩包
（6050）+ 全仓 16 模块 verify 三门 + 台账核账 + 推送封卷。

- 验证：`mvn verify`（根 reactor）BUILD SUCCESS 三门绿；
  TSession6000LedgerAuditTest 全绿。
