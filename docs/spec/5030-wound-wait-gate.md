# Spec 5030 — Wait-Die / Wound-Wait 死锁预防时序裁决（effort #5030，S31）

> wayfinder map：`.wayfinder/maps/effort-5000.md`（T6161–T6162，impl 2181）。
> 借鉴：PostgreSQL/DB2 两阶段锁死锁预防经典（wait-die/wound-wait 时序思想）。
> **勘误（S36 前修复）**：本号原拟 Jump Hash——与 Q 会话
> JumpConsistentHash（spec 3020）同算法同面撞坑；按「占坑即换
> 静脉」纪律换题重落（S31 原提交保留史，本件为现行面）。

## Problem Statement

并发事务抢资源的病：无冲突时序约定时相互等待成环
（死锁——检测与解除风暴）——**时间戳单向等待的预防面**
缺失。

## Solution

`WoundWaitGate`（core/transaction）：

- 事务以**开始时间戳定年龄**（显式注入——确定性无墙钟；
  并列按 id 字典序 tie-break，小者为年长）；
- WOUND_WAIT：年长请求者枪伤（中止）年轻持有者并接管，
  年轻请求者等待；
- WAIT_DIE：年长请求者等待，年轻请求者自裁（中止）——
  两者都保证等待图单向（只等更年轻/只等更年长），环不可
  形成——死锁被预防而非检测；
- `release` 交接年长等待者（时间戳最小者接管）；
  `finish` 注销并释放全部资源；
- 读数：holdersOf/waitersOf（时序可见）/abortedCount
  （预防的代价诚实可见）/transactionCount；
- fail-fast：null/空 id、重复注册、未知事务、重复持有、
  非持有者释放、null mode。

## User Stories

1. 作为调度作者，等待图单向成环不可得——死锁从根预防。
2. 作为审计作者，abortedCount 读数——中止代价可核算。

## Testing Decisions

- 空闲即授予；WOUND_WAIT 年长枪伤（ABORT_HOLDER+接管+
  aborted=1）/年轻等待（WAIT+队列可见）；WAIT_DIE 年轻
  自裁（ABORT_REQUESTOR+注销）/年长等待；release 交接
  年长等待者（队列 [350,400] 取 350）；finish 释放+等待者
  剔除；同时间戳 id 字典序 tie-break（a 枪伤 z）；畸形
  fail-fast。

## Out of Scope

- 不做共享锁多读（本件是排他冲突面）；不做死锁检测器
 （预防面替代检测面）；不做分布式时间戳服务。

## Further Notes

- 与 TwoPhaseCoordinator（spec 5016）同族不同面：原子提交
  vs 冲突时序裁决；与 FencingTokenGuard（S5）不同面：世代
  守卫 vs 死锁预防。
- 里程碑：S31/50（62%）。
