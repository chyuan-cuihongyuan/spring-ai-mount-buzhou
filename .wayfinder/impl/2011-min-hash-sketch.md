# impl 2011 — Q 会话 R11 MinHash Jaccard 素描（spec 3010 / T5021–T5022 / R11）

纵切片：MinHashSketch（core/metrics）——k 路最小哈希签名 + Jaccard
无偏估计 + 确定性派生（复用 DeterministicHash）+ 空集合诚实语义。

- 验证：`mvn -pl buzhou-core test -Dtest='MinHashSketchTest'` 全绿。
