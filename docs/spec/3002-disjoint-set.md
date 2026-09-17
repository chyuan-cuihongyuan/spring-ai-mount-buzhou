# Spec 3002 — 并查集（effort #3002，R3）

> wayfinder map：`.wayfinder/maps/effort-3000.md`（T5005–T5006，impl 2003）。
> 借鉴：Union-Find（路径压缩 + 按秩合并，Tarjan 均摊 α(n)）。

## Problem Statement

归并类语义（会话归并、重复键消解、等价类传递闭包）若用成对标记
或邻接重扫，「A=B、B=C ⇒ A=C」的传递性要么 O(n²) 重扫维护、要么
漏判——需要一个均摊近常量的动态等价类原语。

## Solution

`DisjointSet`（core/concurrent，定容 int 宇宙）：

- `find(x)` 根查找（路径减半——每步跳到祖父，摊还压平树）；
- `union(a, b)` 有效合并 true / 冗余合并 false（账面不动），按秩
  挂接（小组件挂大/等秩下）；
- `connected(a, b)` 同根即连通（传递性由单根结构保证）；
- `componentCount()` 自 capacity 起每有效合并减一（守恒读数）；
- `sizeOf(x)` 组件成员数；越界/负容量 fail-fast。

## User Stories

1. 作为归并作者，成对同源声明后连通性判定免重扫（均摊 α(n)）。
2. 作为对账作者，componentCount 守恒——冗余声明不动账面可审计。

## Testing Decisions

- 只测外部行为不测内部树形（秩/父数组是实现细节）：单例初态、
  链式传递闭包、冗余/自合并 false 且计数不动、计数随有效合并
  单调递减至 1、sizeOf 链聚合、反复 find 稳定（压缩后语义不变）、
  双组件互斥、越界与容量 0 诚实拒绝。

## Out of Scope

- 不做泛型/字符串宇宙（int 定容；外层 Map 映射即可）；不做并行
  合并；不接具体归并业务（接线归后续轮）。

## Further Notes

- 与 VectorClockOrder（因果三态）互补：后者判先后，本件判同源。
- 里程碑：3/150。
