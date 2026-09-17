# impl 2004 — Q 会话 R4 EDF 队列（spec 3003 / T5007–T5008 / R4）

纵切片：EdfScheduler（core/concurrent）——截止期升序 + 同刻 FIFO
tie-break + 空态 +∞ 口径 + headLaxity 余量读数 + Pending 记录。

- 验证：`mvn -pl buzhou-core test -Dtest='EdfSchedulerTest'` 全绿。
