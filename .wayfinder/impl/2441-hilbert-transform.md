# impl 2441 — X 会话 39 HilbertTransform 解析信号包络（spec 10038 / X10077–X10078 / X39）

纵切片：HilbertTransform 解析信号包络——FFT 单边化+逆变换取模的解析包络（core/metrics）。

- 验证：`mvn -pl buzhou-core test -Dtest='HilbertTransformTest'` 全绿。
