# impl 2038 — Q 会话 R39 top-k+温度采样（spec 3038 / T5077–T5078 / R39）

纵切片：TopKSampler（core/policy）——固定宽度截断+温度缩放+读数
对账面。

- 验证：`mvn -pl buzhou-core test -Dtest='TopKSamplerTest'` 全绿。
