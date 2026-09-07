# Spec 205 — 维护模式门（effort #221）

> wayfinder map：`.wayfinder/maps/effort-221.md`（T577–T578）。借鉴：K8s cordon /
> 维护窗口——标记即拒新，存量自然排空。

## Problem Statement

计划内维护（存储迁移/大版本升级/数据修理）需要一个「温和全局闸」：新 Turn
拒绝（用户看到可读的维护文案而非超时/异常），存量轮自然走完——现有工具
各有半边：runtime drain 是硬停机语义（等待并中止），单会话排水管个体——
缺一个轻量的全局「cordon」开关。

## Solution

`MaintenanceGate`（core/session）+ `MaintenanceGateHook`：

- **开关**：`begin(reason, expectedBackAt)` / `end()`——全局 volatile（进程级）。
- **拦截**：beforeTurn（order 10——最高优先级）维护中 block（文案含 reason
  与预计恢复时间）；计数 `buzhou.maintenance.blocked`。
- **组合**：维护序列 = gate.begin（拒新）→ 逐会话 drain 排存量（155）→
  维护作业 → gate.end。
- 不维护零变化（开关关即透传）。

## User Stories

1. 作为运维，维护窗口一行 begin——新用户看到「维护中预计 X 恢复」，存量
   自然结束；end 即恢复。
2. 作为用户，维护期收到可读文案（不是挂死/异常）——体验可控。
3. 作为宿主，gate 与 drain 组合即完整维护流水线——不用自己拼全局逻辑。

## Implementation Decisions

- 纯内存开关（多实例同步留档——进程级维护面先行）。

## Testing Decimals

- 关放行；开拦截文案含 reason/恢复时间；end 恢复；与 drain 协调器组合
  序列（gate 开 → drain 会话 → awaitDrained true）；计数；null 安全。

## Out of Scope

- 自动调度；多实例共享；存量强杀。

## Further Notes

- 维护三件套：门（本轮）+ 排水（155）+ 停机（既有 runtime drain）。
