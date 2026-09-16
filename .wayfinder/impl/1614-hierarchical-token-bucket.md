# impl 1614 — 层级令牌桶（spec 2063 / T3227–T3228 / R64）

纵切片：`HierarchicalTokenBucket`（core/backpressure 主）+
`HierarchicalTokenBucketTest`（六用例）。双闸扣减、父顶硬顶、双封顶。

- 测试：`mvn -pl buzhou-core test -Dtest=HierarchicalTokenBucketTest` 6/6 绿。
- 教训入档：Map.of 数值字面量类型推断（Integer vs Double）；账面断言
  先复演完整扣补序列。
