---
id: T1570
title: skill_search 搜索判定读面的验证裁决
type: task
status: closed
assignee: zcode-j
blocked-by: T1569
created: 2026-09-15
---

## Question

J 会话第 57 轮：SkillSearchStats 读面如何验证？

## Resolution

**用户常设授权 AFK（可推翻）**

验证裁决（SkillSearchStatsTest，SkillRegistry/SessionBindingIndex 内存骨架——骨架见既有 SkillSearchTool 测试）：命中查询 → hits=1；无关查询 → misses=1；坏 JSON → parseRejects=1；空 query → blankQueryRejects=1；守恒 calls = 四桶和；resetForTest 归零。定向 `mvn -pl buzhou-skills -am test -Dtest='SkillSearchStatsTest'` 绿 + 既有 SkillSearchTool 回归绿。
