# impl 1503 — CoefficientOfVariation 波动系数读面（R103 = effort #1902 / spec 1902 / T3005-T3006）

**What**：`CoefficientOfVariation`（core/metrics 静态纯函数 +
Volatility 枚举）——cv（stddev/|mean|，mean=0 fail-fast）+ band
三档（<0.15/<0.5 边界严格小于金融惯例带）；负 stddev fail-fast。

**Why**：金融 CV 语义——绝对波动跨规模不可比（同 stddev 10 在
均值 50 与 5000 下两重天）；无量纲读数 + 三档分档让稳定性评审、
告警分档、跨团队对比同一把尺。与 WelfordAccumulator 互补。

**Verify**：`CoefficientOfVariationTest` 4 用例全绿（三档/边界严格
小于/负均值取绝对值/畸形两型 fail-fast）。

**Status**：done（2026-09-23）
