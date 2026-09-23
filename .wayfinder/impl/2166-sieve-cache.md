# impl 2166 — S 会话 S16 SIEVE 缓存驱逐（spec 5015 / T6131–T6132 / S16）

纵切片：SieveCache（core/cache）——FIFO 环 + 访问位 + 驱逐
指针 + 分叉场景对拍。

- 验证：`mvn -pl buzhou-core test -Dtest='SieveCacheTest'` 全绿。
