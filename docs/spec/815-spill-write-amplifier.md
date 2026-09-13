# 815 — Spill 写放大读数

> 来源：H 会话第 16 轮 = effort #815 / [T1131](../../.wayfinder/tickets/T1131-spill-write-amplifier.md) / [T1132](../../.wayfinder/tickets/T1132-spill-write-amplifier-verify.md) / impl 568。
> 借鉴：RocksDB 写放大口径（≈30K star）。

## Problem

一次 spill store 落 data+meta 双文件、markLinked 再重写 meta——小内容被放大数倍落盘：磁盘预算被 meta 开销吃掉多少不可见（738 是导出域、无落盘域）。

## Solution

`SpillWriteAmplifier`（spill，记账脑+近窗）：

- **累计**：writes/logicalBytes/physicalBytes/amplificationRatio（物理/逻辑）。
- **近窗**：最近 64 次写的 ratio 样本——recentRatio（均值）+ recentP95Ratio（最近秩）反映当前行为（总量被历史稀释时近窗灵敏）。
- **防御**：逻辑 ≤0 忽略（∞ 假象）、物理负忽略；零写入全 0 空真。

## 兼容性

纯新增（喂点归装配侧/调用方——store 主路径零侵入）。

## 诚实边界

记账脑不自动挂钩 DiskSpillStore（读数纪律）；逻辑/字节口径由喂点统一声明；meta 开销占比推导可得不重复发。
