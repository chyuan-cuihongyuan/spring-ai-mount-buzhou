# impl 1435 — EtaProjection 完成度 ETA 投影（R35 = effort #1834 / spec 1834 / T2869-T2870）

**What**：`EtaProjection`（core/eval 静态纯函数）——estimate 线性外推
（rate=done/elapsed，ETA=remaining/rate，向上取整保守）→ Projection
（progress/etaMillis/projectedTotalMillis）；无速率基准（done=0 或
elapsed=0）-1 哨兵；total<1/done 越界/负 elapsed fail-fast。

**Why**：CI 进度条/带宽估计思想——长任务「还要多久」由已观测速率外推，
多时点投影对比可发现尾段速率漂移；无基准不编速率（-1 诚实哨兵）。

**Verify**：`EtaProjectionTest` 5 用例全绿。

**Status**：done（2026-09-16）
