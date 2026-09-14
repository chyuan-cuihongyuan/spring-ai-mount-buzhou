---
id: T1626
title: Skill 管理操作读面的验证裁决
type: task
status: closed
assignee: zcode-j
blocked-by: T1625
created: 2026-09-15
---

## Question

J 会话第 85 轮：SkillAdminStats 读面如何验证？

## Resolution

**用户常设授权 AFK（可推翻）**

验证裁决（SkillAdminStatsTest，InMemorySkillStore 骨架——见 SkillAdminApiTest）：五操作各一 → 五桶各 1；校验异常不入桶；resetForTest 归零。定向 `mvn -pl buzhou-skills -am test -Dtest='SkillAdminStatsTest'` 绿 + 既有 SkillAdminApiTest 回归绿。
