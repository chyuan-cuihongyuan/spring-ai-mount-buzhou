# impl 2117 — R 会话 R17 HNSW 贪心层搜索（spec 4016 / T6033–T6034 / R17）

纵切片：HnswBeamSearch（core/memory）——指数落层 + 贪心下降 +
第 0 层 beam + M 近邻双向连边。

- 验证：`mvn -pl buzhou-core test -Dtest='HnswBeamSearchTest'` 全绿。
