# Spec 3032 — Maglev 哈希（effort #3032，R33）

> wayfinder map：`.wayfinder/maps/effort-3000.md`（T5065–T5066，impl 2033）。
> 借鉴：Google Maglev 负载均衡器（2016 论文，置换表一致性哈希）。

## Problem Statement

键→节点路由（会话分片/端点选择）朴素取模在节点增删时全量重排；
哈希环/jump 已有，但「查表 O(1) 常数 + 显式可控分布 + 最小扰动」
的置换表口径缺位——Maglev 是 Google 一线 LB 验证过的第三条路。

## Solution

`MaglevHash`（core/policy）：

- 每节点步进置换 (offset+j·skip) mod M（种子复用
  DeterministicHash——确定性）；轮询候选抢占查找表空位直至
  填满——**均匀**（份额差 ≤1 槽）+**最小扰动**（增删节点仅
  ~1/n 键迁移）；
- `nodeOf(key)` 查表 O(1)；候选**重复即权重**（占 ~k/n 槽）；
  `entriesOf` 分布对账面；表大小素数校验（置换满周期的前提，
  构造期 O(√M) 试除）。

## User Stories

1. 作为路由作者，节点增删只挪 ~1/n 键——朴素取模全量重排病的
   根治，查表 O(1) 常数级。
2. 作为对账作者，entriesOf 份额可断——分布均匀可证可审计。

## Testing Decisions

- M=13 三节点份额各 ∈[4,5] 且和恰 13；同键跨实例确定性；3→4
  节点千键迁移率 [0.15,0.40]（期望 ~1/4——朴素 ~3/4 对照）；
  重复 3×big vs 1×small 份额比 [2.0,4.5]（期望 ~3×）；千键全
  落登记节点；空候选/非素数/表小于候选/null key 五路 fail-fast。

## Out of Scope

- 不做动态增量重排（节点变更重建表——Maglev 论文同款实践，表
  小重建廉价）；不做故障剔除语义（健康检查归调用方）；不做
  虚拟节点权重参数化（重复即权重已覆盖）。

## Further Notes

- 与 ConsistentHashRing（虚节点环）/ JumpConsistentHash（零内存
  尾端扩缩）成路由三件：环（任意增删）/ jump（零内存）/ Maglev
  （查表常数+可控分布）——按运维形态选型。
- 里程碑：33/150。
