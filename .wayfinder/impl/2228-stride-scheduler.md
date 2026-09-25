# impl 2228 — T 会话 T28 Stride Scheduler 步幅调度（spec 6027 / T6255–T6256 / T28）

纵切片：StrideScheduler（core/concurrent）——pass 单调记账
+最小选取的比例份额（源码随 T24 核账批预入档；初版组合键
记账绕弯重写为 O(n) 最小扫描——客户端数小简洁换正确）。

- 验证：`mvn -pl buzhou-core test -Dtest='StrideSchedulerTest'` 全绿；随 T24 verify 三门绿。
