# Spec 110 — 技能目录注入遥测（effort #72）

> wayfinder map：`.wayfinder72/MAP.md`（T403–T404）。

## Problem Statement

技能面零指标：目录是否在注入（技能体系是否实际生效）、注入是否频繁被预算截断
（catalog-max-entries 过小 → 技能不可见 → 模型不会用）完全不可观测。

## Solution

SkillCatalogRendererImpl.renderEntries 双 counter（BuzhouMetricsHolder）：
- `buzhou.skills.catalog-injected`——每成功注入 +1（注册序/语义排序双路径同源）；
- `buzhou.skills.catalog-overflow`——tag outcome=truncated|fit（两值有界）；
  截断频率是 catalog-max-entries 调优的直接信号。
空目录（binding 无技能）零计数。注入行为零变化。

## User Stories

1. 作为运维，我要截断率可告警，所以预算过小导致的技能不可见可发现。
2. 作为看板作者，我要注入计数，所以技能体系使用面可量化。

## Implementation Decisions

- 计数点在 renderEntries（两条注入路径共用出口）。

## Testing Decisions

- 排序路径预算 2 截 3 → truncated；全量 → fit；空目录零计数。

## Out of Scope

- per-skill 选择计数（进程内表）；skill_search 命中率。

## Further Notes

- 与 spec 59（语义排序）组合：截断率 × 语义命中率 = 预算-相关性联合调优面。
