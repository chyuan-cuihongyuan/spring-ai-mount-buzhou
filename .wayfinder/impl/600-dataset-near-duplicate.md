# impl 600 — DatasetNearDuplicateStats（effort #847）

## 切片

- `buzhou-core/src/main/java/.../core/eval/DatasetNearDuplicateStats.java` — trigramJaccard+并查集（find 路径压缩）+簇统计。
- `buzhou-core/src/test/java/.../core/eval/DatasetNearDuplicateStatsTest.java` — 6 例。

## 口径

- 短串（<3 字符）处理：相等=1.0 否则 0（空集 Jaccard 无定义的工程兜底）。
- duplicatePairs 在未截断时按簇精确计数（Σ size·(size−1)/2）。

## 验证

mvn -pl buzhou-core -am test -Dtest='DatasetNearDuplicateStatsTest' → 6/6 绿；快照再生 1 新公共类型。
