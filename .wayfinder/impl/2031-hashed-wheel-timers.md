# impl 2031 — Q 会话 R31 时间轮定时器（spec 3030 / T5061–T5062 / R31）

纵切片：HashedWheelTimers（core/concurrent）——O(1) 入轮 + 掠槽
到期 + 守恒对账 + 确定性 firing 序。

- 验证：`mvn -pl buzhou-core test -Dtest='HashedWheelTimersTest'` 全绿。
