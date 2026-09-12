# Spec 546 — 技能正文规模审计（effort #546）

> wayfinder map：`.wayfinder/maps/effort-546.md`（T851-852）。E 会话第 46 轮。

## Problem Statement

技能正文是 load_skill 的返回载荷——超预算技能每次加载都吃上下文；
110 目录预算是注入面总量，per-skill 正文规模无审计读数。

## Solution

`skill.SkillBodyAudit`（纯函数）：analyze(skills, budgetChars) →
Report(rows 按 bodyChars 降序+同值字典序稳定，overBudget 标记；stats
聚合 count/total/max/avg/overBudgetCount)；budgetChars≤0 = 不判超限
（只报规模）；null fail-fast。

## User Stories

1. 作为技能作者，我想看哪些技能正文超预算， so 载荷膨胀的技能优先
   拆分或精简。

## Implementation Decisions

- 只审计正文规模（描述/资源列表不计——目录注入面归 110 遥测）。
- 纯读数不拦截。

## Testing Decisions

- 降序+超限标记；零预算不判超限；空表/null fail-fast。

## Out of Scope

- 资源列表规模；自动拦截。

## Further Notes

- 新公共类型 `SkillBodyAudit`（嵌套 `Report`/`SkillRow`）随轮 regenerate
  快照 + api-surface.md 加行。
