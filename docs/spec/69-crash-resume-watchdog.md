# Spec 69 — 崩溃自愈 watchdog（effort #29）

> wayfinder map：`.wayfinder29/MAP.md`（T289–T290）。OSS 借鉴：Temporal worker crash
> 自动接管（research 批判「无 crash watchdog」缺口的正面回应）。

## Problem Statement

进程崩溃后 RUNNING 快照留在注册表；RunRecoveryService 只有手工 restart——需要人知道
崩溃并逐个续跑。研究批判（对照 Temporal/Restate）：checkpoint-only 无 watchdog 不算
durable execution。

## Solution

opt-in `buzhou.recovery.auto-resume`（默认 false）：启动完成后 SmartLifecycle 一次
`autoResumeAll()`——枚举 RUNNING 快照逐一续跑（steal=false：他方活跃实例持锁即跳过；
租约门天然防双接管）；每尝试独立 try/catch；三态计数（resumed/leaseHeld/failed）。

## User Stories

1. 作为运维者，我要崩溃后重启自动接管在途 run，所以不需要人工枚举续跑。
2. 作为多实例运维者，我要活跃实例持锁者不被打扰，所以自愈不会与正常执行打架。
3. 作为红队，我要坏快照失败被隔离计数，所以一个坏读不阻断其余接管。
4. 作为既有用户，我要默认关零变化，所以升级零风险。

## Implementation Decisions

- 启动时一次（SmartLifecycle start）；非周期巡检（fog 留位）。
- 续跑语义 = 既有 restart（悬空修复 + 事件日志回放 + Completed-Turn 检查点）。

## Testing Decisions

- 接管/持锁跳过/零操作/失败隔离四例（InMemoryRunRegistry + 内存 store）。

## Out of Scope

- 周期巡检；分布式选主；auto-resume 上限与退避。

## Further Notes

- 「启动时一次 + 租约门」是保守正确形态——周期巡检需防接管风暴（fog）。
