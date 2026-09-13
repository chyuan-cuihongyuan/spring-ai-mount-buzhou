# 832 — 技能加载延迟读数

> 来源：H 会话第 33 轮 = effort #832 / [T1165](../../.wayfinder/tickets/T1165-skill-load-latency.md) / [T1166](../../.wayfinder/tickets/T1166-skill-load-latency-verify.md) / impl 585。
> 借鉴：LangSmith 延迟分析（使用计数 839 的延迟姊妹面）。

## Problem

技能加载只有计数（SkillUsageStats）：DB 技能正文膨胀/资源解析变慢无量化面——「目录为什么越用越慢」不可查。

## Solution

`SkillLoadLatency`（skills，纯读数）：

- **per-skill 环**：最近 32 次加载样本 + nearest-rank P50/P95 + 全历史 max。
- **溢出桶**：技能数超 1024 并入 `__overflow__` 桶（与 SkillUsageStats 同款口径）。
- **slowest()**：全技能按 P95 降序——最慢排行。

## 兼容性

纯新增（喂点=LoadSkillTool 装配侧）。

## 诚实边界

loads=近窗样本数（累计归 SkillUsageStats）；max 为全历史峰值（环挤出不丢）；喂点手动。
