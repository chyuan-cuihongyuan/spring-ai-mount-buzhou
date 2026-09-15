# impl 1450 — ReservoirSample 水库采样（R50 = effort #1849 / spec 1849 / T2899-T2900）

**What**：`ReservoirSample`（core/observability 静态纯函数）——sample(k,
seed, stream) Knuth 算法 R（前 k 入池、k/i 概率替换——终选概率 k/n 均匀）；
种子化 LCG 可回放；n≤k 全量、k=0 空；负 k/null 元素 fail-fast。

**Why**：Knuth 水库算法思想——诊断采样「攒全量再随机」内存爆炸、「取前
N」到达序偏差；流长未知下均匀采样 + 种子可回放审计。

**Verify**：`ReservoirSampleTest` 4 用例全绿。

**Status**：done（2026-09-16）
