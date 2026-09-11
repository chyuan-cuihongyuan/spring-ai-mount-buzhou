# Wayfinder Map — Buzhou 记忆压缩率分布观测（effort #517，E 会话第 18 轮）

> E 会话第 18 轮（204 fold 计数分布化深挖；416 分位族同法）。勘察：
> 压缩事件（34）有 CompactionListener 缝（reclaimedChars/evictRatio/
> summary/trigger）——**分布观测面**空白：压缩实际回收了多少（分布）、
> 逐出加压到哪级（直方图）、摘要折叠由什么触发（计数）散在事件流里
> 无读数面。

## Destination

`memory.CompactionRatioStats`：挂在既有 CompactionListener 缝上的有界
样本窗（微压缩 512/折入 128 独立双窗）——微压缩回收字符 p50/p95（exact
最近秩、零样本 null）+ totalReclaimed + 逐出比直方图（0.8/0.9/1.0 梯子
级、未知值诚实新键）+ 摘要折入 trigger 计数与折入字符分位。接线：
MemoryModule 静态 STATS（Holder 同型）在既有匿名监听器内双写（观测
零干预）。诚实边界：只观测不干预；重启清零。

## Notes

- 号段：spec 517 / T785–T786 / impl-420。
- 借鉴源：Prometheus histogram / 416 分位族同法。

## Out of scope

- 压缩策略干预；跨进程聚合；per-session 分布。

## Tickets

- [x] [T785 分布样本窗原语](../tickets/T785-compaction-ratio-stats.md)
- [x] [T786 listener 接线](../tickets/T787-compaction-stats-assembly.md)
