# 841 — 会话空闲时长分桶直方

> 来源：H 会话第 42 轮 = effort #841 / [T1183](../../.wayfinder/tickets/T1183-idle-duration-histogram.md) / [T1184](../../.wayfinder/tickets/T1184-idle-duration-histogram-verify.md) / impl 594。
> 借鉴：S5 备选池（IdleSessionMonitor 扩散）。

## Problem

空闲清理只报「清了几个」：空闲时长分布（大多刚闲/大量深闲）决定清理档位是否合理——分布直方缺位。

## Solution

`IdleDurationHistogram`（core.session，纯记账）：

- **固定桶**：可配升序边界（默认 1m/5m/15m/60m→5 桶）；n 边界 n+1 桶；恰达边界归右桶。
- **读数**：桶计数+total+longestIdle+labeledSnapshot（人话区间标签）。
- **防御**：负值忽略。

## 兼容性

纯新增（喂点=Monitor/清理器装配侧）。

## 诚实边界

边界构造期固定；放置法非等宽；喂点手动。
