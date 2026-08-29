---
Type: task
Status: closed
---
## Question

Nginx 平滑加权轮转：比例精确、分布平滑、动态调权。

## Resolution

done（2026-08-30）：impl-313；WeightedRouter（current 累加取最大扣总权重
算法；setWeight 零重建；泛型候选；单锁轻临界区）。
