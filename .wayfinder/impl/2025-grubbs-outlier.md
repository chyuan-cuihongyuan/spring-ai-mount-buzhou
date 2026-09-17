# impl 2025 — Q 会话 R25 Grubbs 离群检验（spec 3024 / T5049–T5050 / R25）

纵切片：GrubbsOutlier（core/eval）——G 统计量 + 内置临界表 +
Welford 矩复用 + 诚实边界。

- 验证：`mvn -pl buzhou-core test -Dtest='GrubbsOutlierTest'` 全绿。
