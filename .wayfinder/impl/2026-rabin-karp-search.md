# impl 2026 — Q 会话 R26 Rabin-Karp 滚动哈希搜索（spec 3025 / T5051–T5052 / R26）

纵切片：RabinKarpSearch（core/metrics）——滚动递推 + 逐字复核 +
hashOf 读数。

- 验证：`mvn -pl buzhou-core test -Dtest='RabinKarpSearchTest'` 全绿。
