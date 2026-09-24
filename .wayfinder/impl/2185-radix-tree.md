# impl 2185 — S 会话 S35 Radix Tree 基数树最长前缀路由（spec 5034 / T6169–T6170 / S35）

纵切片：RadixTree<V>（core/policy）——压缩前缀树 + 边分裂 +
最长前缀匹配（终态与插入序无关钉住）。

- 验证：`mvn -pl buzhou-core test -Dtest='RadixTreeTest'` 全绿。
