# impl 1606 — 有界 Top-K 收集器（spec 2055 / T3211–T3212 / R56）

纵切片：`BoundedTopK`（core/metrics 主）+ `BoundedTopKTest`（七用例）。
守门员逐换、同分保位、门槛读数、双计数。

- 测试：`mvn -pl buzhou-core test -Dtest=BoundedTopKTest` 7/7 绿。
