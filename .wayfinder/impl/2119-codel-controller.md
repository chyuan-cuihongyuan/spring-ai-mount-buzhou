# impl 2119 — R 会话 R19 CoDel 受控延迟队列（spec 4018 / T6037–T6038 / R19）

纵切片：CoDelController（core/backpressure）——sojourn 目标 +
观察窗 + 间隔递缩 + Queue 门面。

- 验证：`mvn -pl buzhou-core test -Dtest='CoDelControllerTest'` 全绿。
