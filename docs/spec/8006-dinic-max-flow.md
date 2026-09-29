# Spec 8006 — DinicMaxFlow 最大流（effort #8006，V7）

> wayfinder map：`.wayfinder/maps/effort-8000.md`（V8013–V8014，impl 2308）。
> 借鉴：Dinic 1970（网络路由带宽规划同源思想）。

## Problem Statement

带宽/管道容量的病：Edmonds-Karp 增广路轮数 O(VE)——
**分层图（level graph）+ 阻塞流（blocking flow）把阶段数
压到 O(V)**，稠密容量网络 O(V²E) 一次成型。

## Solution

`DinicMaxFlow`（core/concurrent，静态工具面）：BFS 分层
（残量 >0 才进层）+ DFS 沿层增 1 阻塞流（当前弧 iter 防
重扫）+ 阶段循环至汇不可达；long 容量域；负容量/源汇
同点/节点越界 fail-fast；边处理按插入序（同图同流值
完全确定——流量分布不承诺唯一，明示）。

## Testing Decisions

- 经典手锚（CLRS 六节点网络→23；桥接图逐值）；最小割
  圣像：100 随机小图（n≤8）vs 全子集枚举割容量取 min
  全等（最大流=最小割定理）；fail-fast。

## Out of Scope

- 不做最小费用流；不还原流量分布（只承诺流值）。

## Further Notes

- 与 BellmanFord（7006）同族不同面：路径代价 vs 容量
  承载上限。
- 里程碑：V7/50（14%）。
