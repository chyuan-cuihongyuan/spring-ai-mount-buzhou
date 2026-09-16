# impl 1609 — 多重比较校正（spec 2058 / T3217–T3218 / R59）

纵切片：`MultipleComparisonCorrection`（core/eval 主）+
`MultipleComparisonCorrectionTest`（七用例）。Bonferroni/Holm 双法、
逐步停步、不弱于证明。

- 测试：`mvn -pl buzhou-core test -Dtest=MultipleComparisonCorrectionTest` 7/7 绿。
