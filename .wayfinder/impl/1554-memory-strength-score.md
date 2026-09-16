# impl 1554 — 记忆强度三分量评分（spec 2003 / T3107–T3108 / R4）

纵切片：`MemoryStrengthScore`（buzhou-memory recall 主）+
`MemoryStrengthScoreTest`（七用例）。recency 2^(−Δt/halfLife) 半衰期、
frequency 对数饱和、importance 钳制、Weights 归一、纯函数零状态。

- 测试：`mvn -pl buzhou-memory test -Dtest=MemoryStrengthScoreTest` 7/7 绿。
- 教训入档：e 指数 vs 2 指数半衰期口径差（e⁻¹≈0.368）——测试钉语义
  先于实现选公式。
