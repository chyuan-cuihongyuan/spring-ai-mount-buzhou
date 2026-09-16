# impl 1595 — 预热斜坡（spec 2044 / T3189–T3190 / R45）

纵切片：`WarmupRamp`（core/backpressure 主）+ `WarmupRampTest`（七用例）。
线性爬升、期满恒满、单调、宽进。

- 测试：`mvn -pl buzhou-core test -Dtest=WarmupRampTest` 7/7 绿。
