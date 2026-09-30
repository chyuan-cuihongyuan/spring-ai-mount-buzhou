# impl 2387 — W 会话 W35 Bezier Curve 贝塞尔曲线（spec 9034 / W9069–W9070 / W35）

纵切片：Bezier Curve 贝塞尔曲线——BezierCurve（core/policy）：de Casteljau 三角逐层插值+折线化。

- 验证：`mvn -pl buzhou-core test -Dtest='BezierCurveTest'` 全绿。
