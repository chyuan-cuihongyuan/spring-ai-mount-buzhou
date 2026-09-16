# impl 1612 — 单调队列滑窗极值（spec 2061 / T3223–T3224 / R62）

纵切片：`SlidingExtremum`（core/metrics 主）+ `SlidingExtremumTest`
（六用例）。压制弹出、序号滑出、双口径。

- 测试：`mvn -pl buzhou-core test -Dtest=SlidingExtremumTest` 6/6 绿。
- 教训入档：单调队列的 size 断言要按「候选=潜在未来极值」模型推——
  直觉的「窗内元素数」是错的。
