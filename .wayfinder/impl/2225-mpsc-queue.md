# impl 2225 — T 会话 T25 MPSC 有界队列（spec 6024 / T6249–T6250 / T25）

纵切片：MpscQueue（core/concurrent）——预分配环+双单调序号
+生产端互斥/消费端无锁。

- 验证：`mvn -pl buzhou-core test -Dtest='MpscQueueTest'` 全绿（MVN_EXIT=0）。
