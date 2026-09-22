# impl 1495 — TailAmplification 尾时延放大读面（R95 = effort #1894 / spec 1894 / T2989-T2990）

**What**：`TailAmplification`（core/metrics 静态纯函数）——
endToEndProbability（q^N 全盒概率）+ requiredPerBoxQuantile
（target^(1/N) SLO 反解）；分位 ∈(0,1]/盒子数 ≥1 fail-fast。

**Why**：The Tail at Scale——单盒 99 分位 × 100 盒并行扇出 → 端到端
仅 36.6%：分位好了就行的直觉在幂次放大下失效；SLO 分解反解单盒
分位（0.99/100 盒 → 4 个 9）。独立性假设诚实入档。

**Verify**：`TailAmplificationTest` 4 用例全绿（经典 0.366/反解 4 个
9/N=1 恒等互逆/畸形三型 fail-fast）。

**Status**：done（2026-09-23）
