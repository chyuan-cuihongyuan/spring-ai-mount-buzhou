# Spec 8018 — IntervalHeap 双端优先队列（effort #8018，V19）

> wayfinder map：`.wayfinder/maps/effort-8000.md`（V8037–V8038，impl 2320）。
> 借鉴：Atkinson, Sack, Santoro & Strothotte 1986 min-max heap（U 系 Wave 9 退雾区遗珠——本轮认领）。

## Problem Statement

双端取最值的病：两个堆双份存储+两堆互删（惰性删除堆积）
——**单数组隐式 min-max 堆：偶层最小层/奇层最大层交错，
双端 O(log n) 一份存储**。

## Solution

`IntervalHeap`（core/concurrent）：数组隐式树（节点 i 的
孩子 2i+1/2i+2），偶层 min-性质/奇层 max-性质；`offer`
上滤（先与同节点对偶值交换定层再上滤）/`pollMin`/`pollMax`
下滤；`peekMin`/`peekMax` O(1)；`size` 读数；null 元素
fail-fast；空堆 poll null 诚实缺省；确定性（同操作序同
布局——并列按先序）。

## Testing Decisions

- 手锚（乱序 10 元 pollMin/pollMax 全序一致性+双端交替）；
  1000 随机 vs TreeMap 圣像（min/max 逐步全等+size 守恒）；
  双端交替不爆栈；fail-fast。

## Out of Scope

- 不做并发锁；不做任意删除（只双端出）。

## Further Notes

- 与 AgingPriorityQueue（exec）同族不同面：老化调度权重
  vs 双端最值数据结构。
- 里程碑：V19/50（38%）。
