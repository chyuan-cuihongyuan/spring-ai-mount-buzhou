# Spec 411 — 泳道优先级原语（effort #411）

> wayfinder map：`.wayfinder/maps/effort-411.md`（T713–T714）。D 会话第 12 轮。

## Problem Statement

泳道是平面公平信号量（FIFO）：全部调用者等价排队——低价值批处理与在线
交互同队，交互被批处理挤压无解。Envoy priority levels 式的「高优先级
插队」在并发原语层缺失。

## Solution

`core.concurrent.PriorityLane`（Envoy priority levels 借鉴）：

- **`acquire(priority, timeout)`**：许可可用立即取；否则入优先级等待
  队列（priority 数小者优先，0-9 有界；同优先级 FIFO 保公平底线）；
  超时 `TimeoutException`（中断响应 InterruptedException）。
- **`release()`**：唤醒队首等待者（跳过所有低优先级后来者——插队
  语义）；无等待者归还许可。
- **`tryAcquire(priority)`**：立即判定（不排队）。
- **`waitingByPriority()`**：各优先级等待数快照（饥饿可见性——观测面）。
- 实现单锁 + PriorityQueue（(priority, seq) 双键排序）+ 条件广播 +
  队首检查（惊群免不了但队首判定 O(log n) 公平）。
- 工具泳道接线（per-tool priority yml）为扩散轮候选——原语先行
  （178→302 原语→装配同节奏）。

## User Stories

1. 作为在线服务作者，我想交互请求优先于批处理拿泳道许可，so 拥挤时
   在线延迟不被批处理拖垮。
2. 作为批处理作者，我想低优先级不饿死（同级 FIFO），so 批任务最终
   能跑完。
3. 作为运维，我想看到各优先级等待数，so 饥饿/挤压可见。

## Implementation Decisions

- 不剥夺：已持有许可者不被抢占（协作式）。
- priority 0-9 有界（IllegalArgumentException 越界——基数纪律）。

## Testing Decisions

- 高优先级插队（P1 排在已等待 P5 前）+ 同级 FIFO；
- 超时让位（低优先级超时退出后高优先级照常）；
- tryAcquire 立即判定；waitingByPriority 快照；并发烟测（多级并发
  acquire/release 不死锁、许可守恒）。

## Out of Scope

- 工具泳道接线；抢占持有者；动态调优先级；同级权重。

## Further Notes

- 新公共类型 `PriorityLane` 随轮 regenerate 快照。
