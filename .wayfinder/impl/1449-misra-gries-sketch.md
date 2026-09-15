# impl 1449 — MisraGriesSketch 频项素描（R49 = effort #1848 / spec 1848 / T2897-T2898）

**What**：`MisraGriesSketch`（core/observability 静态纯函数）——sketch(k,
stream) 计满抵消归零淘汰 + isHeavyCandidate；保证 >N/k 项必幸存、计数
下界（误差 ≤ N/k）；k<2/null 元素 fail-fast。

**Why**：Misra-Gries 流式 heavy hitters 经典算法——全量计数内存随基数
爆炸、采样只给概率答案；O(k) 内存单遍确定性找真热点，计数下界不漏报。

**Verify**：`MisraGriesSketchTest` 4 用例全绿。

**Status**：done（2026-09-16）
