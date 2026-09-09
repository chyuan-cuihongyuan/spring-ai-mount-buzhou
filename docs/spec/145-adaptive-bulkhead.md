# Spec 145 — 自适应并发（effort #101）

> wayfinder map：`.wayfinder/maps/effort-101.md`（T499–T500）。借鉴：TCP AIMD
> （和性增、积性减）+ K8s HPA（按信号调容量）——静态上限的并发闸不知进退。

## Problem Statement

AgentBulkhead 的 per-agent 上限是装配期静态值：下游劣化时高并发只会放大错误率
（不知退让）；深夜低谷时配额闲置（不知释放）。TCP 拥塞控制早已给出答案：
成功时小心加、失败时果断减（AIMD）。

## Solution

`AdaptiveBulkhead`（core/concurrent，动态闸）：

- **AIMD**：每 agent 维护动态上限 limit——连续 N 次（默认 8）成功加性 +1
  （封顶 maxLimit）；任一失败积性减半（floor 1）。
- **获取**：`acquire(agent)` 超<b>动态</b>上限 fail-fast（BuzhouException
  QUOTA_EXCEEDED——与 AgentBulkhead 同词汇）；`Lease` AutoCloseable 归还。
- **喂数**：`recordSuccess(agent)` / `recordFailure(agent)` 公共 API（宿主或
  hook 从调用结局喂数）。
- 观测：`limitOf(agent)` / `snapshot()`（limit/inFlight/blocked/调整次数）；
  计数 `buzhou.adaptive-bulkhead.adjusted`。

## User Stories

1. 作为宿主，下游抖动时并发自动退半再退半（错误率随之受控），恢复后逐步爬回
   ——无需人工改配置重启。
2. 作为运维，snapshot 的 limit 曲线就是系统的「自我调节史」——调参（N/max）有
   数据依据。
3. 作为宿主，静态 AgentBulkhead 与本类并存——想静态配静态、想自适应配自适应。

## Implementation Decisions

- 不改 AgentBulkhead（静态语义保持）；本类是替代装配位。
- per-agent 单锁；无等待语义（fail-fast 信号质量优先——等待在 AgentBulkhead 域）。

## Testing Decisions

- 加性增：N 成功 +1、2N 成功 +2、封顶 max 不越。
- 积性减：一败减半（floor 1）；减后再爬。
- 闸语义：超动态上限拒（QUOTA_EXCEEDED）；Lease 归还后可再取。
- per-agent 隔离；拒绝计数与调整计数。

## Out of Scope

- hook 自动喂数；延迟信号；等待式获取。

## Further Notes

- 舱家族：静态舱（84）/ 集群观测（127）/ 自适应舱（本轮）。
