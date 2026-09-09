# Wayfinder Map — Buzhou outbox due-time 索引（effort #40，50 轮自迭代第 5 轮）

> effort #40，延续 #39（T309–T310 / impl-225）。主线：**#35 fog「outbox due-time
> 键序结构」收口半场**——due() 每轮全量扫 outbox.* 读值解析，退避积压越大读放大
> 越大；spec 78 已铺键序区间读底座，本轮换轨消费。

## Destination

due-time 索引键 `due.<16 位零垫 nextAttemptAt>.<eventId>`（与记录双写）；
`due()` 走 scanByKeyRange 键序区间取最早到期者（limit 按到期序——重退避者不被
挤饿）；孤儿/陈旧索引自愈（就地清键）；构造期回填旧版数据（幂等迁移）；delete/
update/markDead/requeue 出口清键/迁键。行为面：投递语义零变化（at-least-once 契约
内），仅 due() 的 limit 取舍从 seq 优先改为到期优先（显性化定案）。

## Notes

- 借鉴：Kafka log+index / LSM base+index 双结构（双写竞窗用自愈容错，不引分布式事务）。
- 键零垫 16 位十进制：字典序 = 数值序；epoch millis 13 位，余量到 ~2286 年。

## Decisions so far

- limit 按到期序取（旧全量扫按 seq）——防饥饿优先于严格 seq 序（投递顺序本就
  无跨实例保证，spec 24 已记）。
- 陈旧索引不迁移只清除（新键已由 update 写入——幂等安全）。

## Not yet specified

- Redis ZRANGEBYLEX 覆写（换有序结构——需求证据后议）；outbox fsck 增 due 索引
  对账项。

## Out of scope

- 沿用 #7–#39；索引条目 TTL（自愈已覆盖清理语义）。

## Tickets

- [x] [T311 due 索引双写 + due() 换轨 + 回填](../tickets/T311-due-index.md)（impl-226）
- [x] [T312 4 例红队（随迁/最早优先/自愈/出口清键）+ 文档收口](../tickets/T312-due-index-close.md)
