# impl 2153 — S 会话 S3 Fisher-Yates 无偏洗牌（spec 5002 / T6105–T6106 / S3）

纵切片：FisherYatesShuffle（core/policy）——Durstenfeld 无偏
交换 + 三变体 + 种子注入 + 均匀性对拍。

- 验证：`mvn -pl buzhou-core test -Dtest='FisherYatesShuffleTest'` 全绿。
