# Spec 3009 — ARC 自适应替换缓存（effort #3009，R10）

> wayfinder map：`.wayfinder/maps/effort-3000.md`（T5019–T5020，impl 2010）。
> 借鉴：Adaptive Replacement Cache（Megiddo & Modha，IBM）。

## Problem Statement

LRU 单链对工作集「新近敏感」一刀切：一次性扫描流把热键全冲刷
（扫描污染）；LFU 又对频率一刀切（老热键赖着不走）。缺一个在
新近/频率间自适应的通用缓存件。

## Solution

`AdaptiveReplacementCache<K,V>`（core/cache，容量 ≥1 校验）：

- 四链：T1（一次轨道）/ T2（多次轨道）+ B1/B2 幽灵链（逐出键
  记名不记值）；
- 命中（T1/T2/get）一律晋升 MRU-T2；全 miss 进 MRU-T1；
- 幽灵命中自适应：B1 命中（该多保新面孔）p += max(1, |B2|/|B1|)
  上调；B2 命中（该多保热面孔）p −= max(1, |B1|/|B2|) 下调，
  p∈[0, capacity]；
- REPLACE：T1 超 p 逐 T1-LRU 入 B1，否则逐 T2-LRU 入 B2；
- 守恒：实存 ≤ capacity、幽灵各 ≤ capacity；readout
  targetRecency/t1Size/t2Size/b1Size/b2Size 对账面。

## User Stories

1. 作为缓存作者，扫描流不再冲刷热键（一次性键只污染 T1）。
2. 作为对账作者，p 与四链大小全可读——自适应行为可审计。

## Testing Decisions

- 手迹级场景：c=2 新近晋升保护（get 过的键幸存）；c=3 B1 幽灵
  命中 p 0→1 且进 T2；c=10 warm 10 键 + 40 键扫描后 ≥9 存活
  （LRU 对照全灭——扫描抗性主张）；随机 500 op p 恒 ∈[0,4]；
  100 op 幽灵恒 ≤ capacity；容量 0/负 fail-fast；同键更新不增条目。

## Out of Scope

- 不做并发（单线程口径）；不做 TTL/权重变体（GCLOCK/FARC 留白）；
  不做 size-of 统计；不接具体业务缓存（接线归后续轮）。

## Further Notes

- 与频率素描（Caffeine W-TinyLFU 准入）/ CLOCK 族互补：ARC 是
  四链自平衡基线件，素描准入是另一条路。
- 里程碑：10/150。
