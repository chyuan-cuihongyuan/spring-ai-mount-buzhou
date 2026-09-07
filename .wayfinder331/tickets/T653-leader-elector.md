---
Type: task
Status: closed
---
## Question

`LeaderElector` SPI（Leadership{holder,epoch,leader}；try/resign/inspect）
+ InMemoryLeaderElector（单实例语义）+ RedisLeaderElector（Lua 原子
取/续/让，双键 = 持有人 TTL 键 + 单调纪元键）。

## Resolution

done（2026-09-04）：impl-354；SPI 落 core.spi、内存实现落 core.concurrent、
Redis 实现落 store-redis（jedismock EVAL 全可测——双实例竞争/续期纪元
不变/TTL 过期接管纪元递增/非持有人让位无效/inspect 观测）。内存 5 +
Redis 6 用例绿。
