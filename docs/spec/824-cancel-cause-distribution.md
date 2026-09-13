# 824 — 取消原因分布读数

> 来源：H 会话第 25 轮 = effort #824 / [T1149](../../.wayfinder/tickets/T1149-cancel-cause-distribution.md) / [T1150](../../.wayfinder/tickets/T1150-cancel-cause-distribution-verify.md) / impl 577。
> 借鉴：Temporal cancellation 语义观测。

## Problem

会话取消五类原因（606 闭集）只有枚举没有分布：用户取消与租约丢失/失控终止各占比多少不可见——容量规划与异常排查缺「会话为什么死」的一眼面。

## Solution

`CancelCauseDistribution`（core.session，纯记账）：

- **计数+lastSeen**：record(cause, atMillis)；同因 lastSeen 取 max。
- **报告**：counts 降序（cause/count/share/lastSeen）+ total + dominant（平局=声明序先者，EnumMap 确定性）。
- **闭集无界外问题**：枚举固定五值——天然有界。

## 兼容性

纯新增（喂点归取消路径装配侧）；cancel 行为零变更。

## 诚实边界

喂点手动；进程内存有界；声明序平局语义是确定性的（非时序敏感）。
