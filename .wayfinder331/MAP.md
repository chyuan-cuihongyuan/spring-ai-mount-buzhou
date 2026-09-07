# Wayfinder Map — Buzhou 后台任务选主（effort #331，C 会话第 32 轮）

> C 会话第 32 轮。RetentionSweeper（impl-37）是每实例各自调度的后台族
> （会话保留/观测 TTL/摘要修剪 + spill 挂接步骤）：多实例部署时 N 实例
> 重复扫、批量删互相竞争。K8s controller-manager leader election /
> etcd lease 的解法：同一 scope 只有一个执行者，TTL 租约 + 续期 +
> 单调 epoch 围栏（旧主持迟续判失效——与 303 持久纪元同思想）。

## Destination

`LeaderElector` SPI（core.spi：tryAcquireOrRenew/resign/inspect，
`Leadership{holder, epoch, leader}`）+ `InMemoryLeaderElector`（单实例/
测试）+ `RedisLeaderElector`（store-redis：Lua 原子取/续/让，双键 =
持有人 TTL 键 + 单调 epoch 计数键）+ RetentionSweeper 选主门
（非 leader 跳周期留计数、停机让位快速故障转移、手动 sweepOnce 不设门）
+ yml 装配面 `buzhou.leader-election.*`。

## Notes

- 号段：spec 331 / T653–T654 / impl-354。
- 借鉴源：Kubernetes leader election（client-go leaderelection）/ etcd lease。
- 纪律：无 elector bean = sweeper 行为零变化；TTL 由宿主声明（须大于
  sweep 间隔，建议 2×——故障转移时延 ≤ TTL+一周期）；Redis 异常 =
  本周期不扫（失联宁可少做不可抢做——K8s 失租即停执行同语义）。

## Decisions so far

- 续期搭 sweep 周期车（elector 不自持定时器——低频家务不值得一条线程）。
- epoch 计数键无 TTL——跨重启单调，围栏语义诚实。
- 手动 sweepOnce 不设门：运维按钮是人的决定，机器租约不拦人。

## Out of scope

- 通用分布式锁门面（泳道 316 已有；本轮只做「_singleton 后台执行权_」）；
- 主动抢占（candidate 只等 TTL 自然过期——家务不值得抢）；
- watch/通知机制（无 Redis pub/sub 订阅——下周期 try 自然收敛）。

## Tickets

- [x] [T653 LeaderElector SPI + InMemory + Redis Lua 后端](tickets/T653-leader-elector.md)
- [x] [T654 RetentionSweeper 选主门 + 装配 + 收口](tickets/T654-sweeper-wiring.md)
