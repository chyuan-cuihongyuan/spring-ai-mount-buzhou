# impl 2222 — T 会话 T22 Hilbert Curve 希尔伯特曲线（spec 6021 / T6243–T6244 / T22）

纵切片：HilbertCurve（core/policy）——skew 递归双射+局部性
（源码随 T18 对账批预入档；全格穷举双射+单位步局部性钉住）。

- 验证：`mvn -pl buzhou-core test -Dtest='HilbertCurveTest'` 全绿；随 T18 verify 三门绿。
