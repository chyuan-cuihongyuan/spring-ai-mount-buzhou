# Wayfinder Map — Buzhou spawn 优先级调度（effort #87，新 50 轮会话第 2 轮）

> effort #87，承接 #86。fog 种子②「优先级调度（SpawnGate）」：spawn 排队纯 FIFO，
> VIP 会话（运维接管/付费租户）无法插队。

## Destination

SpawnGate 三级优先级排队（HIGH/NORMAL/LOW）：高优先级抢占低级排队者、同级 FIFO、
默认调用零行为变化（NORMAL）。借鉴 OS 调度多级队列 + Envoy 优先级面。

## Notes

- 本轮由 B 会话认领（并行会话分工见 .wayfinder86 MAP 登记）；本轮文件面：
  backpressure/SpawnPriority（新）+ SpawnGate（改写）+ 新测试。
- 改写口径：公平信号量 → 锁 + 每级票据队列有向交接（释放时空位直达最高级队首，
  杜绝新到者加塞）。

## Decisions so far

- 有向交接（ticket handoff）而非 signalAll 抢跑：释放者直接授予最高级队首票据。

## Not yet specified

- runtime 配置面（按会话声明优先级的 yml 键）——后续轮按需。

## Out of scope

- 沿用 #7–#86；动态优先级/老化（aging）。

## Tickets

- [x] [T445 SpawnPriority + SpawnGate 三级排队改写](../tickets/T445-spawn-priority.md)（impl-272）
- [x] [T446 优先级调度回归（插队/同级 FIFO/防加塞/默认等价）](../tickets/T446-spawn-priority-tests.md)（impl-272）
