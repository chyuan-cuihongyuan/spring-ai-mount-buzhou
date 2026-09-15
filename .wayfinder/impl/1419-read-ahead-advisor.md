# impl 1419 — ReadAheadAdvisor 顺序读预读顾问（R19 = effort #1818 / spec 1818 / T2837-T2838）

**What**：`ReadAheadAdvisor`（buzhou-spill 静态纯函数）——尾链检测（相邻读
首尾相接）三态 SEQUENTIAL/RANDOM/COLD；SEQUENTIAL 预读 = blockSize×
2^min(链长−1, 3)（封顶 8 倍）；RANDOM 零预读；COLD 样本不足。

**Why**：Linux readahead 思想——固定预读窗两头亏（顺序扫描往返多、随机
点查白读挤缓存）；访问形状驱动窗大小自适应。

**Verify**：`ReadAheadAdvisorTest` 4 用例全绿。

**Status**：done（2026-09-16）
