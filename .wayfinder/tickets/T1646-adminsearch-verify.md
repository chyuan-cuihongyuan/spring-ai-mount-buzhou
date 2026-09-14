---
id: T1646
title: SkillAdmin×Search 可见性联动组合测试轮的验证裁决
type: task
status: closed
assignee: zcode-j
blocked-by: T1645
created: 2026-09-15
---

## Question

J 会话第 95 轮：管理搜索联动如何验证？

## Resolution

**用户常设授权 AFK（可推翻）**

验证裁决（SkillAdminSearchComboTest，InMemorySkillStore 骨架）：发布后搜索命中（hits 增）→ 下架后搜索消失（misses 增）→ 双读面各自守恒。定向 `mvn -pl buzhou-skills -am test -Dtest='SkillAdminSearchComboTest'` 绿。
