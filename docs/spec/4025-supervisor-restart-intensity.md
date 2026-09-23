# Spec 4025 — 监督者重启强度（effort #4025，R26）

> wayfinder map：`.wayfinder/maps/effort-4000.md`（T6051–T6052，impl 2126）。
> 借鉴：Erlang/OTP supervisor max_restarts/max_intensity。

## Problem Statement

「无限重启循环把故障刷成噪音」——子进程重启频率的累积判定与
升级件缺失。

## Solution

`SupervisorRestartIntensity`（core/runaway，时钟可注入）：

- 滑窗内至多 maxRestarts 次子重启——越限即**闩锁升级**
 （OTP 语义：终止全部子进程并停监督者）；
- 闩锁保持到显式 reset（重启的监督者开新纪元从头计）；
- restartsInWindow 滑窗读数（先淘汰再读，可审计）。

## User Stories

1. 作为监督作者，重启风暴升级停机——故障不刷成噪音。
2. 作为运维作者，强度阈值与窗口可配、读数可审计。

## Testing Decisions

- 恰满 3/3 不越；第 4 次越限闩锁 + 窗滑走后闩锁仍保持；
  滑窗淘汰（now−t ≥ window 全出）+ 新窗重算；reset 新纪元；
  畸形三型 fail-fast。

## Out of Scope

- 不做重启策略档（one_for_one/rest_for_one/all_for_one——动作归
  调用方）；不做子进程真生命周期；不做指数退避（退避族已覆盖）。

## Further Notes

- 与 TurnStallWatchdog（单轮失速）互补：本件管重启**频率**累积。
- 里程碑：26/50。
