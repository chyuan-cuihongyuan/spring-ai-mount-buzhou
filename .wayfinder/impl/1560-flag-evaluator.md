# impl 1560 — 特性开关求值器（spec 2009 / T3119–T3120 / R10）

纵切片：`FlagEvaluator`（core/policy 主）+ `FlagEvaluatorTest`（七用例）。
五态 reason、永不抛出、谓词兜底、分布计数。

- 测试：`mvn -pl buzhou-core test -Dtest=FlagEvaluatorTest` 7/7 绿。
- 教训入档：record 不变量必须守在 compact constructor——工厂守约可被
  canonical 构造绕过。
