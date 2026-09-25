# Spec 6037 — Stable Bloom Filter 稳定布隆过滤器（effort #6037，T38）

> wayfinder map：`.wayfinder/maps/effort-6000.md`（T6275–T6276，impl 2238）。
> 借鉴：Stable Bloom 思想。源码本轮入档。

## Problem Statement

流式成员判定的病：标准/计数布隆一旦插入永久占据（「最近
见过」语义无法表达）——**隐式时间衰减面**缺失。

## Solution

`StableBloomFilter`（core/metrics，源码已预载）：

- 每次插入前将 d 个确定性游标位衰减 −1，再按 k 哈希置满
  （饱和 3）；查询判 k 位全非零——旧元素停插即淡出；
- SplitMix64 游标+双哈希探针（确定性——同流同行为）；
- fail-fast：槽位/衰减/哈希数越域、null。

## User Stories

1. 作为去重作者，事件流「最近见过」判定自动遗忘旧事件。
2. 作为审计作者，衰减参数显形——遗忘速率可调。

## Testing Decisions

- 1000 近期插入全程恒真（无假阴性）；20000 噪声流后旧成员
  淡出；重复插入流中持续可见；双实例行为一致；fail-fast
 （参数放宽用例 20000/4096/3 钉住）。

## Out of Scope

- 不做删除（衰减即遗忘）；不做容量自适应。

## Further Notes

- 与 CountingBloomFilter（6036）同族不同面：显式删除 vs
  隐式时间衰减。
- 里程碑：T38/50（76%）。
