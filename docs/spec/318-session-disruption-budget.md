# Spec 318 — 会话扰乱预算（effort #318）

> wayfinder map：`.wayfinder318/MAP.md`（T627–T628）。借鉴：Kubernetes
> PodDisruptionBudget——voluntary disruption 需预算，minAvailable 保底。

## Problem Statement

计划性维护/实例缩容排空会话没有预算面：一次全排 = 服务中断窗口；多个维护
动作并发排水无互斥——可用会话数可以被打到零。

## Solution

`SessionDisruptionBudget`（core.session）：

- 额度 = ACTIVE 数 − minAvailable；排水前 `tryAcquireDisruption()` 原子预留
  一格（available − reserved &gt; minAvailable 才放行——预留制防并发超发）；
  排水完成 `completeDisruption()` 归还。
- 非主动扰乱（会话异常终结）不占额度——预算只管 voluntary 排水。
- yml：`buzhou.session.disruption-budget.min-available`（默认 0 = 不限，不装配
  零变化；依赖会话索引——无索引不装配）。计数器（allowed/rejected）。

## User Stories

1. 作为运维，维护窗口保底 50 个可用会话——排空别的实例也不打穿服务。
2. 作为运维，并发维护动作共享预留池——不会两个脚本各排一半凑成全排。

## Implementation Decisions

- 预算进程内（部署单元语义；跨实例共享族已收口 316——预算按实例即按部署单元）。

## Testing Decisions

- `SessionDisruptionBudgetTest`：额度随预留递减/触底拒绝/归还恢复/0 不限/
  计数器；装配测试：min-available&gt;0 且索引在场才装配。

## Out of Scope

- 自动排水执行器；跨实例预算共享。

## Further Notes

- 维护族：排水（155）/ 维护门（205）/ **扰乱预算（本轮）**——K8s 三件套齐。
