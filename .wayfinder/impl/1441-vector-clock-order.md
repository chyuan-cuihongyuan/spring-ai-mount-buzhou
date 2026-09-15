# impl 1441 — VectorClockOrder 向量时钟偏序（R41 = effort #1840 / spec 1840 / T2881-T2882）

**What**：`VectorClockOrder`（core/concurrent 静态纯函数）——compare 三态
因果判（BEFORE/AFTER/CONCURRENT，缺席按 0、相等退化 BEFORE）；负分量
fail-fast。

**Why**：Dynamo 向量时钟/Lamport happens-before 思想——墙钟偏移下「谁先」
不可判；CONCURRENT 判定让真冲突（无因果写）进冲突解、有因果序的直接
取新弃旧。

**Verify**：`VectorClockOrderTest` 5 用例全绿。

**Status**：done（2026-09-16）
