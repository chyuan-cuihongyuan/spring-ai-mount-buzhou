# Wayfinder Map — Buzhou 共享 Redis 语义向量缓存（effort #91，B 会话第 3 轮）

> B 会话第 3 轮（原拟 #88，A 会话先建 MAP 得 88/89/90——分工协议生效，本轮落 91）。
> fog 种子③「RediSearch 向量缓存」：spec 55 语义缓存进程内线性扫描，多实例各持一份。

## Destination

Redis 共享语义向量缓存（buzhou-store-redis）：桶 = hash 键、客户端 cosine 最近邻、
跨实例共享命中；RediSearch FT.SEARCH 服务端 KNN 为演进路径（vanilla Redis 兼容先行）。

## Notes

- **B 侧票号自 T471 起偏移 +20 防撞**（A 侧顺延 T451+；#86/#88 两轮撞号由收口轮
  台账归一——A 会话 .wayfinder88 MAP 已登记此约定）。
- **B 会话后续轮次主题预告（A 会话请避让）**：#92 跨实例分布式舱/表聚合
  （fog④）、#93 PII yml 声明式规则（fog⑤）、#94 归档 autoconfig 定时（fog⑥）、
  #95 前缀缓存命中率 provider 侧（fog⑦）、#96 工具级熔断（resilience4j
  per-tool CircuitBreaker）。
- 借鉴 RediSearch 向量索引语义（index=桶、cosine KNN、TTL），存储层可移植 HASH。
- 故障语义：后端不可达 = 旁路 miss + bypass 计数（缓存 fail-open——与限流
  fail-fast 刻意不对称，风险不对称）。

## Decisions so far

- 客户端 KNN 先行（FT.SEARCH 需 redis-stack 模块，服务端演进留档）。

## Not yet specified

- resilience 侧 SemanticCacheAdvisor 接入共享后端；FT.SEARCH 服务端 KNN。

## Out of scope

- 沿用 #7–#90；HNSW/分层索引（量级不到）。

## Tickets

- [x] [T471 RedisSemanticVectorCache 共享桶存储 + cosine KNN](../tickets/T471-redis-semantic-cache.md)（impl-275）
- [x] [T472 容器测试：跨实例命中/阈值 miss/惰性过期/容量驱逐/旁路](../tickets/T472-redis-semantic-tests.md)（impl-275）
