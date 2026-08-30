# Spec 98 — Redis 键序区间覆写（effort #59）

> wayfinder map：`.wayfinder59/MAP.md`（T365–T366）。#39 fog 项收口。

## Problem Statement

scanByKeyRange（spec 78）三栈中 Redis 走默认实现：scanByPrefix 全量值读（批量
HGETALL）→ 内存过滤排序——Redis 部署（outbox due 索引等时间编键结构）没吃到
下推红利。

## Solution

`RedisSessionStateStore.scanByKeyRange` 覆写：SMEMBERS 键侧过滤（前缀 + 含界下界 +
排他上界）→ TreeMap 排序截断 limit → 仅命中键 `batchHgetAll`（既有流水线，一次
往返）。顺序保证与契约一致（键字典序，契约测试三栈同断言）。

诚实取舍：不引入 ZSET（ZRANGEBYLEX 需每 put 维护第二结构——写路径双写成本大于
读路径键侧过滤已消的值读大头；迁移另议）。

## User Stories

1. 作为 Redis 部署运维，我要 due 索引读下推，所以调度路径与 JDBC 同级放大消减。
2. 作为红队，我要契约同断言，所以三栈语义不漂移。

## Implementation Decisions

- 复用 batchHgetAll 流水线（spec 58 先例——命中键一次往返）。
- jedis-mock 内嵌单测（testcontainers 之外的轻量补位——CI 无 docker 也可跑）。

## Testing Decisions

- jedis-mock：键序升序/排他上界/含界下界/limit 截断/值回读正确/非法 limit。

## Out of Scope

- SET→ZSET 结构迁移；Redis 侧 due 审计。

## Further Notes

- with spec 79：outbox due() 在 Redis 部署下的每拍读从「全量值读」降为
  「命中值读」。
