# Spec 1801 — Spill 压力失速读面（effort #1801，R2）

> wayfinder map：`.wayfinder/maps/effort-1800.md`（T2803–T2804，impl 1402）。借鉴：
> Linux 内核 PSI（Pressure Stall Information）——压力不看水位看「谁在等」，
> some 档（≥1 任务失速）量吞吐损失 / full 档（全部非空闲任务失速）量进度损失。

## Problem Statement

Spill 现有读面答「溢写了多少、读回了多少」，答不了「运行时被 spill 拖得多
疼」：阈值水位高不等于会话真的在等磁盘；偶发溢写与全局阻塞对体验是两种病
（前者加余量、后者换存储），现状没有分档读数。

## Solution

`SpillPressureStall`（buzhou-spill，静态纯函数）：

- `StallSample(activeSessions, stalledSessions)` 单窗事实（紧凑构造器核契约：
  0 ≤ stalled ≤ active，畸形 fail-fast）；
- `analyze(samples)` → `PsiReport(windows, someWindows, fullWindows,
  worstStalled, worstActive)`：some 计 ≥1 失速窗、full 只计活跃全失速窗
  （空闲窗进分母不进分子）；
- `somePct()`/`fullPct()`/`worstStallRatio()` 读数（空观测 -1 哨兵）。

## User Stories

1. 作为运维者，somePct=0.75 / fullPct=0.5 直接读出「四分之三时间有会话在等、
   一半时间全体在等」——分档定位是加阈值余量还是换更快存储。
2. 作为容量规划者，峰值窗（worstStalled/worstActive）回答最坏时刻的失速面
   有多宽。
3. 作为框架宿主，采样口径（轮边界/溢写 tick）自声明，读面零侵入。

## Implementation Decisions

- 纯读面零状态，不改 SpillService 阈值判定；只读不裁决。
- 契约校验在 record 构造器（fail-fast 先于账目）；null 按空表。
- 峰值并列取失速面更宽的窗（信息量更大）。

## Testing Decisions

- 账目正确（some/full 分档 + 峰值窗）；空闲窗只进分母；空表/null 哨兵；
  畸形样本 fail-fast；峰值并列取宽窗。外部行为断言（比值），不测内部。

## Out of Scope

- 不接 Micrometer 指标、不做自动阈值调整（归宿主策略）。

## Further Notes

- 与 SpillTieringAudit（冷热分层）正交：那是「读分布」，这是「等分布」。
