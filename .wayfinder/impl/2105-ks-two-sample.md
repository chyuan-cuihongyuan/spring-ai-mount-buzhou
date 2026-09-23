# impl 2105 — R 会话 R5 KS 两样本检验（spec 4004 / T6009–T6010 / R5）

纵切片：KsTwoSample（core/eval，纯静态）——ECDF 双指针 D 统计量 +
渐近 Kolmogorov p（NR 小样本校正）。

- 验证：`mvn -pl buzhou-core test -Dtest='KsTwoSampleTest'` 全绿。
