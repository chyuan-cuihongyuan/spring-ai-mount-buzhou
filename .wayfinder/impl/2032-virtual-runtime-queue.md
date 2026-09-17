# impl 2032 — Q 会话 R32 vruntime 公平队列（spec 3031 / T5063–T5064 / R32）

纵切片：VirtualRuntimeQueue（core/concurrent）——按权折算虚拟时钟
+ TreeMap 取最小 + 确定性并列序。

- 验证：`mvn -pl buzhou-core test -Dtest='VirtualRuntimeQueueTest'` 全绿。
