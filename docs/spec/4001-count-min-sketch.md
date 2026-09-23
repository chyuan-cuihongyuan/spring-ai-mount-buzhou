# Spec 4001 — Count-Min 素材计数（effort #4001，R2）

> wayfinder map：`.wayfinder/maps/effort-4000.md`（T6003–T6004，impl 2102）。
> 借鉴：Cormode-Muthukrishnan 2005；Cassandra/ClickHouse 内建 CountMinSketch。

## Problem Statement

「每个键精确记一个 long」的内存在基数爆炸场景（工具签名/错误签名/
查询词频）不可持续；但 MisraGries 只保 k 个候选——**任意键点查**无解。

## Solution

`CountMinSketch`（core/metrics，d×w 计数矩阵 + 确定性行散列）：

- increment(key,n) 全行加记；estimate(key) d 行取最小；
- 非负增量下**只高估不低估**（单侧误差，ε·N 概率界由 d/w 配平）；
- totalCount 精确守恒账（与行列噪声无关）；
- 行散列复用 DeterministicHash 基值 + 行黄金比步进 splitmix——
  确定性可回放。

## User Stories

1. 作为观测作者，错误签名计数不再被基数爆炸吃内存——固定内存
   换全键可查。
2. 作为对账审计者， totalCount 守恒 + 单侧不低估合同可测。

## Testing Decisions

- 单键精确（零碰撞零噪声）+ 未见键为零；百键并发不低估且上界
  ≤ 真值+N/8；加权增量直读 + 维度读数；畸形四型 fail-fast
  （depth/width<1、负 n、null key——increment 与 estimate 各半）。

## Out of Scope

- 不做合并（merge）/降权（decay）/保守更新（CU）变体；
  不做负计数（delete）语义。

## Further Notes

- 与 MisraGriesSketch 成对（要名单 vs 要点查）；与
  HllCardinalitySketch 互补（去重基数 vs 带重计数）。
- 里程碑：2/50。
