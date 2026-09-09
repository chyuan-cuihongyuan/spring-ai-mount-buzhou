# Spec 349 — C 会话收官终验（effort #349）

> wayfinder map：`.wayfinder/maps/effort-349.md`（T689–T690）。C 会话第 50 轮，
> 收官轮（无新机制——R30 半程收口同型）。

## Problem Statement

R31–R49 十九轮机制逐轮验证（受影响模块绿 + 快照随轮），但缺一次
整reactor 的收口终验与台账归档。

## Solution

- 全 reactor 串行测试（`mvn test` 全模块 + 本地 Windows 排除集——
  CI/Linux 权威口径不变）。
- spec↔README 覆盖门 + API 快照比对（Skipped: 0）复验。
- PROGRESS.md 归档 50/50；会话记忆更新（跨窗口恢复锚点）。

## User Stories

1. 作为贡献者，我想收官轮证明 19 轮增量整体无红，所以 半程到收官
   的防线是闭环的。
2. 作为下一位会话恢复者，我想 PROGRESS 归档明确 50/50 完成，所以
   不会重复开工已收官的号段。

## Out of Scope

- 新机制（本轮零代码——纯验证与归档）；push（网络窗口另议）。

## Further Notes

- C 会话 300 系就此收官：50 轮 wayfinder→spec→tickets→implement
  闭环，efforts #300–#349、specs 300–349、tickets T591–T690、
  impls 323–372。
