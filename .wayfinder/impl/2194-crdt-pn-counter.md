# impl 2194 — S 会话 S44 CRDT PN-Counter 正负计数器（spec 5043 / T6187–T6188 / S44）

纵切片：CrdtPnCounter（core/transaction）——P/N 双 G-Counter
单调表 + max 合并三律 + 乱序同步收敛钉住。

- 验证：`mvn -pl buzhou-core test -Dtest='CrdtPnCounterTest'` 全绿（MVN_EXIT=0）。
