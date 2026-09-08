# Wayfinder Map — Buzhou 泳道优先级原语（effort #411，D 会话第 12 轮）

> D 会话第 12 轮。勘察（2026-09-08）：ToolLaneRegistry = 平面公平
> Semaphore（FIFO）——**无优先级语义**：全部调用者等价排队，低价值批处
> 理挤占在线交互的困境无解。Envoy priority levels / OS 调度优先级的
> 「高优先级插队」在并发原语层缺失。
> 勘察旁注：审计 WORM 主题勘察发现 AuditRecordStore 本身已 append-only
> 设计——增益薄弃。

## Destination

`core.concurrent.PriorityLane`（Envoy priority levels 借鉴）：有界许可 +
优先级插队——`acquire(priority, timeout)`：许可被占时入优先级等待队列
（priority 数小者优先；同优先级 FIFO 保公平）；release 唤醒队首等待者
（跳过所有低优先级）；超时 TimeoutException（等待位置让位后来高优先级）；
`tryAcquire(priority)` 立即判定；`waitingByPriority()` 观测快照（各优先级
等待数——饥饿可见性）。装配面（LaneLimitingToolCallback 接线优先级）
为扩散轮候选——本轮原语先行（178→302 原语→装配同节奏）。

## Notes

- 号段：spec 411 / T713–T714 / impl-384。
- 借鉴源：Envoy priority levels（流量分级）+ Java 公平信号量语义（同级
  FIFO 防饿死的底线）。
- 纪律：priority 0-9 有界（基数纪律）；等待队列无界=调用者已受上游
  泳道超时约束（诚实边界）。

## Out of scope

- 工具泳道接线（yml per-tool priority——扩散候选）；优先级抢占已持有
  许可者（不剥夺——协作式）；动态调优先级；权重（同级公平即可）。

## Tickets

- [x] [T713 PriorityLane 原语](../tickets/T713-priority-lane.md)
- [x] [T714 抢占/超时/观测语义](../tickets/T714-priority-lane-semantics.md)
