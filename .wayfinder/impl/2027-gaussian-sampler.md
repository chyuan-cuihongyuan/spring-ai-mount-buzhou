# impl 2027 — Q 会话 R27 Box-Muller 高斯采样器（spec 3026 / T5053–T5054 / R27）

纵切片：GaussianSampler（core/policy）——极坐标变换 + 成对产出 +
平移缩放 + 回放。

- 验证：`mvn -pl buzhou-core test -Dtest='GaussianSamplerTest'` 全绿。
