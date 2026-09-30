# Spec 9002 — Edmonds-Karp BFS 增广最大流（effort #9002，W3）

> wayfinder map：`.wayfinder/maps/effort-9000.md`（W9005–W9006，impl 2355）。
> 借鉴：Edmonds–Karp 1972（Ford-Fulkerson 的 BFS 最短路化——网络带宽分配同源）

## Problem Statement

DFS 盲目增广的病：找路不按长度，可被精心
构造的长路反复绕行钉成指数步——**BFS 只走弧数最少的
增广路**：每条边至多饱和 O(V) 次，总共 O(VE²) 封顶。

## Solution

EdmondsKarpMaxFlow（core/concurrent，静态工具面）：
maxFlow(n,edges{from,to,cap},source,sink) 返回流值；邻接
矩阵容量（平行边合并）；流值唯一（分布不唯一明示）；
源汇同点/端点越域/负容量 fail-fast。

## Testing Decisions

菱形网经典手锚（5）；平行边合并；割断瓶颈；方向断路；
反向回流收口；60 随机图与 DinicMaxFlow 流值互证全等；
确定性双跑；fail-fast。

## Out of Scope

不做分层阻塞流（Dinic 面）；不做最小费用流；
不做流分布提取（流值承诺）。

## Further Notes

与 DinicMaxFlow（8006）同族不同面：逐条 vs 阶段。
Wave 1 图匹配与割族第二件。
