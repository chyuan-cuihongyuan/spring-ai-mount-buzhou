# Spec 124 — 虚拟 key 配额（effort #88）

> wayfinder map：`.wayfinder/maps/effort-88.md`（T449–T450）。#85 fog 种子⑨「虚拟 key
> 配额」收口。借鉴：LiteLLM virtual-key budgets。

## Problem Statement

多租户/多下游 key 共用一个模型入口时没有 per-key 的 token 分账限流：一个 key
的失控消耗挤占全体预算，账单只能事后从日志反推。

## Solution

`budget/VirtualKeys`：每 key 一个 token 硬顶的进程内有界注册表（256 封顶满则
fail-fast）。`trySpend` 单 AtomicLong CAS 原子扣减——加完后越限整体拒绝、不留
部分扣减；`spendOrThrow` 折既有 `QUOTA_EXCEEDED`（NON_RETRYABLE）；`usage`/
`topUsage` 井读快照（账单与告警面）；`reset` 窗口清零（export → reset 循环，
spec 121 同纪律）。未注册 key 直通（不设预算 = 不拦——默认关哲学的诚实边界）。
拒绝计数 `buzhou.virtual-keys.rejected`（无 tag——key 名天生无界）。

## User Stories

1. 作为平台运维，我给每个下游 key 设 token 硬顶，所以单 key 失控不挤占全体
   预算，越限即刻结构化拒绝。
2. 作为财务，我按窗口导出用量再清零，所以每窗口一份 per-key 账单、表永有界。

## Testing Decisions

- 红队：限额内累计与快照；越限原子拒绝（已用不涨）+ 结构化异常文案锚点；
  未注册 key 直通 + usage 空值诚实；注册 fail-fast 四象限（空白/非正/重复/
  表满）；reset 后恢复可用；负数拒绝 + 零额 noop。

## Out of Scope

- TokenBudgetHook 接线（key 解析面）；成本 micro-USD 配额；远端 key 仓同步；
  per-key 限速。

## Further Notes

- 与 spec 16（会话级三硬顶）正交：那是 per-session 预算闸，这是 per-key 分账。
