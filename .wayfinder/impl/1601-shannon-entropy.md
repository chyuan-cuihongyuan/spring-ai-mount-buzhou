# impl 1601 — 香农熵读数（spec 2050 / T3201–T3202 / R51）

纵切片：`ShannonEntropy`（core/metrics 主）+ `ShannonEntropyTest`
（七用例）。bits/nats 双口径、归一化、零频惯例。

- 测试：`mvn -pl buzhou-core test -Dtest=ShannonEntropyTest` 7/7 绿。
