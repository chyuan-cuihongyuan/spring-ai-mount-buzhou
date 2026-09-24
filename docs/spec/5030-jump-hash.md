# Spec 5030 — Jump Hash 跳跃一致哈希（effort #5030，S31）

> wayfinder map：`.wayfinder/maps/effort-5000.md`（T6161–T6162，impl 2181）。
> 借鉴：Google Lamping-Veach 跳跃一致哈希论文（一致哈希生态/Envoy 同面思想）。

## Problem Statement

分片路由的病：哈希环 + 虚拟节点需要建环与每节点元数据
（内存/重排开销），朴素 `hash % n` 扩容时几乎全量键迁移
（n→n+1 变模）——**极简内存 + 单调稳定迁移面**缺失。

## Solution

`JumpHash`（core/policy，静态工具类）：

- `bucketOf(long key, int bucketCount)`：论文算法——线性同余
  发生器逐桶随机游走，桶数 m 增至 m+1 时约 m/(m+1) 的键
  原桶不动（单调稳定——扩容只搬最少量的键）；
- `bucketOf(String key, int)` + `fingerprint(String)`：
  FNV-1a 64 稳定指纹入口（跨进程同键同桶；
  `String.hashCode` 不承诺跨进程稳定且仅 32 位）；
- fail-fast：bucketCount ≤0、null 键。

## User Stories

1. 作为分片作者，无环无每节点状态——常数内存定桶。
2. 作为扩容运维者，扩桶只迁移 ~1/(m+1) 键——重排开销最小。

## Testing Decisions

- 论文算法圣像值钉住（Python 64 位语义预演：bucketOf(1,10)=6、
  bucketOf(42,100)=43 等）；确定性（同键同桶两次全等）；
  单调稳定（1000 键 5→6 留桶 ≥800，理论 ≈833）；均衡
  （1000 键 10 桶 max/min<2，理论 ≈1.23）；指纹三键圣像 +
  字符串/长整入口一致性；畸形参数 fail-fast。

## Out of Scope

- 不做负载上限约束（BoundedLoadRing 已覆盖有界负载面）；
  不做环/虚拟节点结构（本件是极简无状态面）；不做键重映射
  的持久化协调。

## Further Notes

- 与 BoundedLoadRing（spec 5003）同族不同面：极简无状态跳跃
  vs 有界负载环。Wave 6 第一件。
- 里程碑：S31/50（62%）。
