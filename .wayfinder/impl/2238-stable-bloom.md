# impl 2238 — T 会话 T38 Stable Bloom Filter 稳定布隆过滤器（spec 6037 / T6275–T6276 / T38）

纵切片：StableBloomFilter（core/metrics）——确定性游标衰减
位阵（淡出可观测性由噪声流/槽数比调参钉住）。

- 验证：`mvn -pl buzhou-core test -Dtest='StableBloomFilterTest'` 全绿（MVN_EXIT=0）。
