# Wayfinder Map — Buzhou 自适应并发（effort #101，B 会话第 13 轮）

> B 会话第 13 轮。主题池「自适应并发」：AgentBulkhead 上限是静态配置——突发
> 错误率上升时不知退让，低谷时不知释放。借鉴 K8s HPA（按信号调容量）与 TCP
> AIMD（和性增/积性减）。

## Destination

AdaptiveBulkhead（core/concurrent）：per-agent 动态并发闸——成功每 N 次加性
+1（封顶 max），失败积性减半（地板 1）；acquire 超动态上限 fail-fast
（QUOTA_EXCEEDED 同语义）；limit 变迁与拒绝计数可观测。

## Notes

- 号段：B=奇数 spec（本轮 145）。
- 与 AgentBulkhead（静态）并存：自适应是替代装配位，不改既有类。
- AIMD 信号 = 调用结局（成功/失败）——宿主或 hook 侧喂数（本轮先状态机+闸）。

## Decisions so far

- 积性减半比 -1 保守（错误信号代价高——TCP 同理由）。

## Not yet specified

- hook 自动喂数接线；RTT/延迟信号面。

## Out of scope

- 沿用 #7–#100；等待式获取（AgentBulkhead 已有等待语义）。

## Tickets

- [x] [T499 AdaptiveBulkhead（AIMD 动态闸）](../tickets/T499-adaptive-bulkhead.md)（impl-285）
- [x] [T500 自适应回归（加性增/积性减/上限下限/拒绝计数）](../tickets/T500-adaptive-tests.md)（impl-285）
