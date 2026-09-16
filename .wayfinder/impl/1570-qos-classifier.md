# impl 1570 — QoS 资源声明分级（spec 2019 / T3139–T3140 / R20）

纵切片：`QosClassifier`（core/policy 主）+ `QosClassifierTest`（七用例）。
三态分级、混维拉低、驱逐序、让位判定。

- 测试：`mvn -pl buzhou-core test -Dtest=QosClassifierTest` 7/7 绿。
