# impl 1565 — 键压缩日志语义（spec 2014 / T3129–T3130 / R15）

纵切片：`KeyCompaction`（core/recovery 主）+ `KeyCompactionTest`
（九用例）。maxSeq 胜、墓碑语义、乱序幂等、压缩率对账。

- 测试：`mvn -pl buzhou-core test -Dtest=KeyCompactionTest` 9/9 绿。
