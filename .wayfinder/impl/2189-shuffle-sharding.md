# impl 2189 — S 会话 S39 Shuffle Sharding 洗牌分片（spec 5038 / T6177–T6178 / S39）

纵切片：ShuffleSharding（core/policy）——租户×分片子集
种子化分配 + 子集路由 + 重叠读数（圣像钉住）。

- 验证：`mvn -pl buzhou-core test -Dtest='ShuffleShardingTest'` 全绿（MVN_EXIT=0）。
