# impl 2140 — R 会话 R40 DDSketch 相对误差分位（spec 4039 / T6079–T6080 / R40）

纵切片：DdSketch（core/metrics）——γ 对数桶 + 分位扫桶 +
merge + min/max 精确旁路 + fail-fast。

- 验证：`mvn -pl buzhou-core test -Dtest='DdSketchTest'` 全绿。
