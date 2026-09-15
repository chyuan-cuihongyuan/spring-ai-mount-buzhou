# impl 1429 — SmoothWeightedSequence 平滑加权轮询（R29 = effort #1828 / spec 1828 / T2857-T2858）

**What**：`SmoothWeightedSequence`（core/exec 静态纯函数）——NGINX smooth
WRR 逐字算法（current 累加-峰值派出-回收总权重）生成派发序 + counts 直方；
零权重不参与；确定性；畸形四型 fail-fast。

**Why**：NGINX smooth weighted round-robin 思想——朴素 WRR 按权连派产生
负载锯齿；平滑加权让 5:1:2 派发 aabacaad 而非 aaaaabc，比例与平滑两全、
连接复用高。

**Verify**：`SmoothWeightedSequenceTest` 5 用例全绿（首跑编译红为 lambda
捕获循环变量，布尔局部量修正）。

**Status**：done（2026-09-16）
