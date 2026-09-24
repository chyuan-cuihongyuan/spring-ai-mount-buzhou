# impl 2190 — S 会话 S40 Segmented LRU 分段缓存（spec 5039 / T6179–T6180 / S40）

纵切片：SlruCache<K,V>（core/cache）——双段 LRU + 晋升/降级/
淘汰三分面 + 扫描不污染钉住。

- 验证：`mvn -pl buzhou-core test -Dtest='SlruCacheTest'` 全绿（MVN_EXIT=0）。
