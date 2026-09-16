# impl 1575 — 老化优先级队列（spec 2024 / T3149–T3150 / R25）

纵切片：`AgingPriorityQueue`（core/exec 主）+ `AgingPriorityQueueTest`
（八用例）。等待生息、同分 FIFO、快照观测、O(n) 诚实边界。

- 测试：`mvn -pl buzhou-core test -Dtest=AgingPriorityQueueTest` 8/8 绿。
- 教训入档：同速率老化不改变相对差——反超场景必须构造等待时长差。
