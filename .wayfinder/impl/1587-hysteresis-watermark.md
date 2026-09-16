# impl 1587 — 双阈值迟滞水位门（spec 2036 / T3173–T3174 / R37）

纵切片：`HysteresisWatermark`（core/backpressure 主）+
`HysteresisWatermarkTest`（七用例）。高停低续、保持区、翻转计数。

- 测试：`mvn -pl buzhou-core test -Dtest=HysteresisWatermarkTest` 7/7 绿。
