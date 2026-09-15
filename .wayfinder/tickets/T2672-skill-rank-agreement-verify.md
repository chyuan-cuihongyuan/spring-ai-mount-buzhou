---
id: T2672
title: 技能排序一致性读面的验证门
type: task
status: closed
assignee: zcode-l
blocked-by: T2671
created: 2026-09-15
---

## Question

SkillRankAgreement 怎么验证？（spec 1735 验收/裁决）

## Resolution

SkillRankAgreementTest：全一致+1/全相反−1/不相交与单项哨兵/部分重叠只看公共项。
