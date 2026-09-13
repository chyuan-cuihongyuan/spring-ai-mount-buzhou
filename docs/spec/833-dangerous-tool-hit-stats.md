# 833 — 危险工具命中分布

> 来源：H 会话第 34 轮 = effort #833 / [T1167](../../.wayfinder/tickets/T1167-dangerous-tool-hit-stats.md) / [T1168](../../.wayfinder/tickets/T1168-dangerous-tool-hit-stats-verify.md) / impl 586。
> 借鉴：WAF top-rules 观测（命中热力排行惯例）。

## Problem

危险工具被 HITL 拦截后即流走：「哪个危险工具被碰得最多」无热力排行——安全画像与目录治理（该下架谁）缺数据。

## Solution

`DangerousToolHitStats`（guard.config，纯读数）：

- **per-tool 命中**：hits/requiredState（最近非空）/lastSeen(max)；工具封顶 64 超限并入溢出桶。
- **排行**：top(n) 命中降序（典序破平）；totalHits/distinctTools 总口径。
- **喂点**：GuardHook 命中处装配侧——拦截行为零变更。

## 兼容性

纯新增；DangerousToolGuardHook 零变更。

## 诚实边界

requiredState 不存历史；溢出桶不可回溯；命中计数不等于拦截计数（未配 hook 时仅统计）。
