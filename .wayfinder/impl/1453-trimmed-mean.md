# impl 1453 — TrimmedMean 截尾均值（R53 = effort #1852 / spec 1852 / T2905-T2906）

**What**：`TrimmedMean`（core/eval 静态纯函数）——mean(samples, f) 排序
双侧各截 ⌊n×f⌋ 再均；f∈[0,0.5)；零截退化算术均；空表/null -1 哨兵；
null/NaN 样本 fail-fast。

**Why**：统计学 trimmed mean/体育评审惯例思想——均值被离群值劫持（30s
卡顿拉爆中枢）、中位数对序信息免疫过头；去头尾再平均两全，评分清洗与
延迟汇报同形状共用一件基建。

**Verify**：`TrimmedMeanTest` 4 用例全绿（首跑红为哨兵期望误——f<0.5
截空数学不可达，修正后绿）。

**Status**：done（2026-09-16）
