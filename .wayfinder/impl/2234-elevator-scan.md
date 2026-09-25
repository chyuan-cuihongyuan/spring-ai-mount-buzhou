# impl 2234 — T 会话 T34 Elevator Scan 电梯扫掠（spec 6033 / T6267–T6268 / T34）

纵切片：ElevatorScan（core/policy）——显式磁头+方向 LOOK
扫掠（源码随 T30 对账批预入档）。

- 验证：`mvn -pl buzhou-core test -Dtest='ElevatorScanTest'` 全绿；随 T30 verify 三门绿。
