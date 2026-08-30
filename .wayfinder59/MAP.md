# Wayfinder Map — Buzhou Redis 键序区间覆写（effort #59，50 轮自迭代第 24 轮）

> effort #59，延续 #58（T363–T364 / impl-244）。主线：**#39 fog 项「Redis 键序
> 结构覆写」**——scanByKeyRange 三栈中 Redis 走默认实现（scanByPrefix 全量值读
> 排序），Redis 侧部署（outbox due 索引 etc.）没吃到下推红利。

## Destination

`RedisSessionStateStore.scanByKeyRange` 覆写：SMEMBERS 键侧过滤（前缀+区间）+
TreeMap 排序截断 + 命中键 batchHgetAll 批量取值（免全量值读——limit 小时省一个
量级往返）；顺序保证与契约一致。jedis-mock 内嵌单测补位（键序/上下界/limit/值
回读/非法 limit）。

## Notes

- 借鉴：既有 spec 58 batchHgetAll 流水线纪律复用；不引 ZSET（双结构维护成本 >
  收益——键侧过滤已消值读大头）。

## Decisions so far

- 不用 ZRANGEBYLEX（需每 put 维护 ZSET——写路径加双结构；键侧 SMEMBERS 过滤已
  消值读大头，诚实取舍入档）。

## Not yet specified

- SADD→ZADD 全量迁移（写路径换结构——大迁移另议）；outbox due 审计的 Redis 版。

## Out of scope

- 沿用 #7–#58；键集 SET 结构变更。

## Tickets

- [x] [T365 Redis scanByKeyRange 覆写](tickets/T367-redis-range.md)（impl-245）
- [x] [T366 jedis-mock 红队（键序/上下界/limit/值回读）+ 收口](tickets/T368-redis-range-close.md)
