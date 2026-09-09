# Wayfinder Map — Buzhou 轮次时延计时（effort #214，B 会话第 37 轮）

> B 会话第 37 轮。eval run 有 duration timer（spec 111），<b>生产轮次</b>没有——
> 「本 agent 的轮要多久」散在模型 timer（若有）与工具 timer（108）里，端到端
> 轮时延缺位（含重试/工具/压缩全链）。

## Destination

TurnTimingHook（core/hook）：beforeTurn 记起点（会话内存表）、afterTurn 计
端到端时长喂 buzhou.turn.duration timer（tag agent 有界）+ 单会话最近 N 次
均值/最大（观测读数）。挂 hook 即计时。

## Notes

- 号段：B=奇数 spec（本轮 191）；轮次 .wayfinder200+。
- 起点/终点在 hook 面（beforeTurn→afterTurn 之间的完整轮）——重试内含。
- 表 per-session LRU 1024（内存纪律）。

## Decisions so far

- 单会话滚动窗（最近 64 次）供 avg/max 读数。

## Not yet specified

- 直方图桶配置；慢轮事件（超阈值告警外发）。

## Out of scope

- 沿用各轮；跨会话聚合（那是指标面板域）。

## Tickets

- [x] [T563 TurnTimingHook（端到端计时+滚动窗读数）](../tickets/T563-turn-timing.md)（impl-309）
- [x] [T564 计时回归（时长正确/滚动窗/隔离/未完轮安全）](../tickets/T564-turn-timing-tests.md)（impl-309）
