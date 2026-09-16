# impl 1571 — 冷启动豁免 φ 嫌疑门（spec 2020 / T3141–T3142 / R21）

纵切片：`GraceAwareFailureDetector`（core/concurrent 组合件主）+
`GraceAwareFailureDetectorTest`（七用例）。φ×豁免合流、滤噪分流、
三态判定。

- 测试：`mvn -pl buzhou-core test -Dtest=GraceAwareFailureDetectorTest` 7/7 绿。
- 教训入档：φ 模型需 ≥2 间隔样本 + 远超均值沉默才出高值——测试节奏
  要按模型口径设计，不是按直觉拍时刻。
