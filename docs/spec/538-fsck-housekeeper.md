# Spec 538 — store fsck 定时巡检（effort #538）

> wayfinder map：`.wayfinder/maps/effort-538.md`（T829–T830）。E 会话第 38 轮。

## Problem Statement

StoreFsck 对账面只有手工触发——store 衰变/部分失败残余在两次手工 fsck
之间静默累积。定时巡检（341 选主门）空白。

## Solution

`cleanup.StoreFsckHousekeeper`（SmartLifecycle）：周期 StoreFsck.run（只读）
→ findings>0 WARN+`buzhou.fsck.{runs,findings}` 计数（不自动修复——repair
归手工面）；yml `buzhou.fsck.{enabled, interval}`（enabled 默认关；
interval 默认 6h）；elector 缺席=无门单实例跑（ArchivePurgeJob 同语义）。

## User Stories

1. 作为运维，我想 store 不一致（孤儿摘要/残留 state）被定时巡检发现，
   so 衰变在 restore/purge 前可见（WARN+计数）。

## Implementation Decisions

- 只读巡检不自动修复（删除动作必须显式——341 家务纪律）。
- elector 缺席=无门单实例跑（有 bean 即门）。

## Testing Decisions

- 干净 store findings 0；孤儿摘要（摘要+观测、无消息）巡出；生命周期
  起停；yml enabled 装配/缺席。

## Out of Scope

- 自动 repair；健康面接入。

## Further Notes

- 新公共类型 `StoreFsckHousekeeper`、`BuzhouFsckProperties` 随轮
  regenerate 快照 + api-surface.md 加行。
