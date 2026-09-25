# impl 2237 — T 会话 T37 Counting Bloom Filter 计数布隆过滤器（spec 6036 / T6273–T6274 / T37）

纵切片：CountingBloomFilter（core/metrics）——k 哈希计数数组
可删除布隆（计数 −1 饱和于 0；假阳性经验界钉住）。

- 验证：`mvn -pl buzhou-core test -Dtest='CountingBloomFilterTest'` 全绿（MVN_EXIT=0）。
