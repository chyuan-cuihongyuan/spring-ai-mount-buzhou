# Spec 125 — 共享 Redis 语义向量缓存（effort #91）

> wayfinder map：`.wayfinder/maps/effort-91.md`（T471–T472）。#85 收口 fog 种子③
> 「RediSearch 向量缓存」。借鉴：RediSearch 向量索引（index / cosine KNN / TTL）
> ——存储层以 vanilla Redis HASH 可移植实现，服务端 FT.SEARCH 为演进路径。

## Problem Statement

语义缓存（spec 55）是进程内存储：多实例部署下每实例各持一份——实例 A 刚学会的
FAQ 相似命中，实例 B 要重新问一遍模型（再付一次模型调用）。跨实例共享语义缓存
需要共享存储与向量最近邻查询，原 runbook 明示「跨实例共享 out-of-scope
（RediSearch 另议）」——本 spec 落这颗种子。

## Solution

`RedisSemanticVectorCache`（buzhou-store-redis）：

- **布局**：桶（modelName + options 采样，与 spec 55 同口径）= 一个 Redis HASH
  （`<prefix><bucket>`）；field = entryId；value = JSON（embedding 数组、payload、
  expireAt、seq）。
- **查询**：`findNearest(bucket, queryEmbedding)` —— HGETALL 桶内客户端 cosine
  最近邻，≥ 阈值命中返回（entryId、payload、similarity）；桶内量级数百与进程内
  线性扫描同级成本（spec 55 perf 哨兵口径）。
- **写入**：`put(bucket, entryId, embedding, payload, ttl)` —— 惰性清过期 →
  容量驱逐（最低 seq 最旧先出，LRU-by-seq）→ HSET + 桶键 EXPIRE 刷新。
- **过期**：双层——条目 expireAt 惰性判定（命中路径清除）+ 桶键 TTL 兜底回收。
- **故障语义（与限流后端刻意不同）**：Redis 不可达 = 旁路 miss + bypass 计数，
  **不 fail-fast**——缓存失效的风险是「多付一次模型调用」，限流失效的风险是
  「打爆上游」，风险不对称故语义不对称（限流 fail-fast / 缓存 fail-open）。
- 计数：hit / miss / bypass 三计数（`buzhou.semantic.redis.*`）。

## User Stories

1. 作为多实例部署的宿主，实例 A 缓存的 FAQ 语义命中，实例 B 的同义问法直接
   命中共享缓存——同问法跨实例零模型调用。
2. 作为运维，Redis 抖动时对话不受影响（缓存旁路、直通模型），bypass 计数
   让旁路率可观测。
3. 作为宿主，桶容量与 TTL 限制共享缓存的内存占用（驱逐最旧、双层过期）。

## Implementation Decisions

- 客户端 KNN（cosine 零范数防护、维度不匹配跳过——与进程内版同防护）。
- embedding 以 JSON float 数组存储（数百维 × 数百条量级可接受；二进制紧凑编码
  留给量级升级时）。
- 不改 `SemanticCacheStore` / `SemanticCacheAdvisor`（resilience 接入面下一轮按需）。

## Testing Decisions

- Testcontainers `redis:7-alpine`（先例 RedisSessionIndexContractTest；无 Docker 跳过）。
- 只测外部行为；跨实例场景 = 两个独立连接的 cache 实例一写一读。
- 五面：跨实例命中 / 阈值下 miss / 惰性过期（sleep 过 TTL）/ 容量驱逐最旧 /
  连接关闭旁路 miss 不抛。

## Out of Scope

- FT.SEARCH 服务端 KNN（redis-stack 模块依赖，演进路径）；HNSW 索引；
  resilience Advisor 接入面与 yml 配置。

## Further Notes

- 与 spec 55 正交：进程内版仍为默认；本类是共享层增量。
