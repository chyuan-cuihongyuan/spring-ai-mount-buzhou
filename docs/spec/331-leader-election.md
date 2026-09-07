# Spec 331 — 后台任务选主（effort #331）

> wayfinder map：`.wayfinder/maps/effort-331.md`（T653–T654）。C 会话第 32 轮。

## Problem Statement

保留清理族（RetentionSweeper：会话保留、观测 TTL、摘要修剪、
ToolCallLog/RunRegistry 窗口、spill 挂接步骤）是每实例各自调度的后台
执行器。多实例部署时每个实例都在扫：重复工作浪费存储往返，批量删
（MaintenanceTrigger 限量）互相竞争，报告噪声翻 N 倍。「同一时刻整个
集群只需要一个执行者」缺乏机制承载。

## Solution

`LeaderElector` SPI（core.spi）+ 双实现 + RetentionSweeper 选主门：

- **SPI**：`Leadership tryAcquireOrRenew()`（幂等——已是 leader 则续）、
  `void resign()`（主动让位——停机快速故障转移）、`Leadership inspect()`
  （观测面——当前持有者/纪元，不尝试获取）。`Leadership{holder, epoch,
  leader}`——epoch 单调递增围栏纪元（303 持久纪元同思想）。
- **InMemoryLeaderElector**（core.concurrent）：单实例/测试语义——
  try 即 leader、resign 即空位、再取纪元递增。
- **RedisLeaderElector**（store-redis）：Lua 原子取/续（持有人匹配续
  TTL；空位即取并 INCR 纪元键；他人持有返回跟随）；resign 仅持有人
  匹配才 DEL。双键：持有人键（PSETEX TTL）+ 纪元计数键（无 TTL——
  跨重启单调）。
- **RetentionSweeper 门**：调度周期先 tryAcquireOrRenew——非 leader
  跳过本周期（计数留痕），leader 执行；Redis 异常 = 本周期不扫
  （失联宁可少做不可抢做）；stop() 主动 resign（快速故障转移）；
  手动 `sweepOnce()` 不设门（运维按钮是人的决定）。
- 装配：`buzhou.leader-election.{enabled,ttl,holder-id}`（store.type=redis
  且 enabled=true 才供 Redis elector bean；core 经 ObjectProvider 消费，
  无 bean = sweeper 行为零变化）。TTL 须大于 sweep 间隔（建议 2×）；
  故障转移时延 ≤ TTL + 一个周期。

## User Stories

1. 作为多实例部署的使用者，我想同一时刻只有一个实例执行保留清理，所以
   存储往返不翻倍、批量删不互相竞争。
2. 作为运维，我想 leader 实例停机时主动让位（resign），所以 故障转移
   不必等 TTL 自然过期。
3. 作为运维，我想 leader 失联（Redis 异常）时本周期宁可跳过清理，所以
   分区脑裂下不会出现双执行者。
4. 作为审计者，我想每次「非 leader 跳过」留计数，所以 清理长期没跑
   能被定位是选主还是策略原因。
5. 作为运维，我想随时 inspect 当前 leader 与纪元，所以 无需登 Redis
   就能回答「现在谁在扫」。
6. 作为使用者，我不想配置选主时 sweeper 行为与现状完全一致，所以
   升级零风险。
7. 作为运维，我想手动 sweepOnce 不受选主限制，所以 紧急清理时任何
   实例都能立即执行。
8. 作为贡献者，我想围栏纪元跨重启单调递增，所以 旧持有人迟到续期
   可被判定失效（不会复活旧纪元）。

## Implementation Decisions

- 续期搭 sweep 周期车：elector 不自持定时器（低频家务不值得一条线程）；
  候选实例同样每周期 try——TTL 过期后自然接管。
- epoch 计数键不设 TTL：跨重启单调，INCR 只发生在新获取时（续期不增）。
- Redis elector 独占 Lettuce 连接（与 315/316 后端同模式），装配时
  holder-id 缺省自动生成（实例身份），scope 固定 `buzhou`（家务族
  一个 leader）。
- 失败语义：取/续 Lua 抛异常 = 本周期跳过（上游 catch 吞并留 WARN）——
  与 K8s 失租即停执行同语义。

## Testing Decisions

- InMemory：取/续/让/再取纪元递增、inspect 语义。
- Redis（jedismock，EVAL 探测跳过——真 Redis 语义归 CI Testcontainers）：
  双实例竞争唯一获取、持有人续期纪元不变、TTL 过期（DEL 模拟）后
  候选取权纪元递增、非持有人 resign 无效、跨实例（两 backend 同键）
  接管。
- Sweeper 门：leader 执行/非 leader 跳过+计数/Redis 异常跳过/stop 让位/
  手动不设门。
- 先例：RedisVirtualKeyBudgetBackendTest（jedismock 模式）、
  RetentionSweeper 既有测试。

## Out of Scope

- 通用分布式锁门面（泳道 316 已有——本轮只做 singleton 后台执行权）；
- 主动抢占与优先级（candidate 等 TTL 自然过期）；
- leader 变更事件/通知（watch/pub-sub——下周期 try 自然收敛）；
- 其他后台任务（ArchivePurgeJob/IdleCompactionHousekeeper）接线
  （等本轮模式验证后再扩散）。

## Further Notes

- 新公共类型（LeaderElector/InMemoryLeaderElector/RedisLeaderElector）
  随轮 regenerate API 快照。
