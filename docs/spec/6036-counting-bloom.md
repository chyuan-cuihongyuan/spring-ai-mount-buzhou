# Spec 6036 — Counting Bloom Filter 计数布隆过滤器（effort #6036，T37）

> wayfinder map：`.wayfinder/maps/effort-6000.md`（T6261 续 / impl 2237）。
> 借鉴：Bloom 1970 计数变体思想。占坑勘误：原拟 GkSketch 简化
> 实现秩界经验不可自洽——按「不可自洽即换静脉」纪律移回雾区。

## Problem Statement

标准布隆位阵的病：不可删除（移除元素只能重建）——**可删除
计数位阵面**缺失。

## Solution

`CountingBloomFilter`（core/metrics，源码已预载）：

- k 哈希计数数组：插入 +1、删除 −1（饱和于 0），contains
  判定 k 位计数全 >0——无假阴性、可删除；
- 假阳性率 ≈(1−e^{−kn/m})^k（经验受控）；SplitMix64 双
  哈希 h1+i·h2 模拟 k 探针（确定性）；
- fail-fast：预期插入≤0、假阳性率越域、哈希数越域、null。

## User Stories

1. 作为缓存作者，穿透过滤支持元素失效删除——动态成员面。
2. 作为容量作者，假阳性率参数化——内存/精度可调。

## Testing Decisions

- 500 插入零假阴性+删一插一回）；单元素删除即缺席；重复
  插入需等量删除才消失；2000 探针假阳性 ≤5%；参数越域
  fail-fast。

## Out of Scope

- 不做计数饱和告警（int 计数上界远超用例）；不做自动
  扩容。

## Further Notes

- 与 SessionBloomFilter/XorFilter（5042）同族不同面：可
  删除动态计数 vs 位阵/静态指纹。
- 里程碑：T37/50（74%）。
