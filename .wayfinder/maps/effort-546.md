# Wayfinder Map — Buzhou 技能正文规模审计（effort #546，E 会话第 46 轮）

> E 会话第 46 轮（skills 模块首触轮；110 目录预算的 per-skill 深化）。
> 勘察：技能正文是 load_skill 的返回载荷——超预算技能每次加载都吃
> 上下文；110 目录预算是注入面总量，per-skill 正文规模无审计读数。

## Destination

`skill.SkillBodyAudit`（纯函数）：analyze(skills, budgetChars) →
Report(rows 降序+overBudget 标记，stats 聚合 count/total/max/avg/
overBudgetCount)；budgetChars≤0 = 不判超限（只报规模）。

## Notes

- 号段：spec 546 / T839-840 → 实际 T847-848 已用；本票 T851-852 / impl-448。
- 借鉴源：110 目录预算遥测（Backstage catalog score 同族）。

## Out of scope

- 资源列表规模；自动拦截；目录注入面遥测（110 已有）。

## Tickets

- [x] [T851 审计原语](../tickets/T851-skill-body-audit.md)
- [x] [T852 边界语义](../tickets/T852-skill-audit-edge.md)
