# impl 2014 — Q 会话 R14 扫线最大并发（spec 3013 / T5027–T5028 / R14）

纵切片：SweepLineIntervals（core/metrics）——事件扫线 + 半开语义 +
首发点 + 空集哨兵。

- 验证：`mvn -pl buzhou-core test -Dtest='SweepLineIntervalsTest'` 全绿。
