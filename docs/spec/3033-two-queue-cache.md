# Spec 3033 — 2Q 双队列缓存（effort #3033，R34）

> wayfinder map：`.wayfinder/maps/effort-3000.md`（T5067–T5068，impl 2034）。
> 借鉴：Two-Queue（Johnson & Shasha 1994，简化 2Q 无幽灵版）。

## Problem Statement

LRU 对一次性扫描流全 cache 冲刷（热键被扫走）；ARC 用幽灵自适应
解决但结构较重——2Q 用**静态分诊**更简：新面孔先进 FIFO 观察窗，
二触才升主 LRU——扫描流永远只在观察窗自旋。

## Solution

`TwoQueueCache<K,V>`（core/cache，泛型）：

- 入口 A1in（FIFO，容量 inbound）+ 主区 Am（LRU，容量 = 总−
  inbound）；
- get：Am 命中重排；A1in 命中**晋升 Am**（主区满则 LRU 逐出）；
  miss null；
- put：Am 更新重排；A1in 内更新即晋升；新键入 A1in（FIFO 满
  整删最老）；
- 双逐出计数对账（inboundEvictions/mainEvictions）；总容量 ≥2、
  inbound ∈[1, 总−1] 校验。

## User Stories

1. 作为缓存作者，扫描流不再冲刷热键——一次性键只在观察窗打转。
2. 作为对账作者，双区逐出分账——分诊行为可审计。

## Testing Decisions

- 命中/未命中/入口内更新即晋升；50 写总容量恒 ≤4；二触晋升手迹
  （c=4 in=2：get a 后 4 键冲洗，a 存活 b 亡）；扫描抗性（c=10
  in=5：warm 后主区终存 6..10，20 键扫描全打转，5 键全存活）；
  10 写入口逐出恰 8 主区零逐出；容量三路 fail-fast。

## Out of Scope

- 不做 A1out 幽灵（完整 2Q 留白——简化版已保扫描抗性）；不做
  动态比分（ARC 领地）；不做 TTL/权重；不做并发。

## Further Notes

- 与 AdaptiveReplacementCache 成对：2Q 静态比分简、ARC 自适应
  重——按工作集稳定性选型；CLOCK 是零分诊最低摩擦档——三档
  覆盖维护成本谱系。
- 里程碑：34/150。
