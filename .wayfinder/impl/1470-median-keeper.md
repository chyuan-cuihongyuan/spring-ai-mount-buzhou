# impl 1470 — MedianKeeper 流式中位数保持器（R70 = effort #1869 / spec 1869 / T2939-T2940）

**What**：`MedianKeeper`（core/metrics，synchronized）——双堆对半结构：
add O(log n) 平衡不变量 + median O(1)（奇小半顶/偶双顶均值，空 -1 哨兵）
+ size；NaN/Inf fail-fast。

**Why**：双堆中位数经典结构思想——观测流的当前中枢排序法每次 O(n
log n)、均值无抗偏；双堆流式保持 O(log n) 入 O(1) 读、长尾不拉走。

**Verify**：`MedianKeeperTest` 4 用例全绿。

**Status**：done（2026-09-16）
