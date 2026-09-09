# Wayfinder Map — Buzhou 崩溃自愈 watchdog（effort #29）

> effort #29，延续 #5–#28（累计 175 轮 / T1–T288 / impl 1–214）。
> 主线：**opt-in 崩溃自愈**——研究批判（Diagrid/Temporal 对照）指出的「无 crash
> watchdog」缺口：RunRecoveryService 只有手工 restart；进程崩溃后 RUNNING 快照需要
> 人来枚举续跑。借鉴 Temporal crash watchdog：启动时自动接管疑似崩溃者。

## Destination

`buzhou.recovery.auto-resume=true`（默认关）+ RunRegistry bean 时：启动完成后
SmartLifecycle 一次 `autoResumeAll()`——枚举 RUNNING 快照逐一续跑（steal=false：他方
活跃实例持锁即跳过不打扰；每尝试独立隔离）；三态计数（resumed/leaseHeld/failed）入
日志；默认关零变化；新键 1 个登记 metadata 与矩阵。

## Notes

- 外部事实源：Temporal worker crash 自动接管（workflow 任务超时重派）；本地裁定：
  启动时一次（非周期巡检——周期巡检 fog 留位），租约门保证不打扰活跃实例。

## Decisions so far

- watchdog 在 core autoconfig（SmartLifecycle start 一次）；service 面新增 autoResumeAll。
- 失败隔离 per-run（一个坏快照不阻断其余）。

## Not yet specified

- 周期巡检（运行中定期接管）；auto-resume 上限/退避（量级证据后议）。

## Out of scope

- 沿用 #7–#28；周期调度；分布式选主（租约门已够单接管语义）。

## Tickets

- [x] [T289 autoResumeAll（三态计数 + 失败隔离 + 租约门跳过）](../tickets/T289-watchdog.md)（impl-215）
- [x] [T290 autoconfig SmartLifecycle 接线 + 新键 + 红队 + 收口](../tickets/T290-watchdog-close.md)
