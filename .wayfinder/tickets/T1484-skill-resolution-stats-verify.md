---
id: T1484
title: 技能解析未命中计数读面的验证裁决
type: task
status: closed
assignee: zcode-j
blocked-by: T1483
created: 2026-09-14
---

## Question

J 会话第 17 轮：解析未命中计数如何验证？

## Resolution

**用户常设授权 AFK（可推翻）**

验证裁决（SkillResolutionStatsTest，复用 ClasspathSkillScanner 骨架；命中名从 listAllFor 动态取——不硬编码测试资源技能名）：已知技能 load → resolved=1；幻觉名 load → notFound=1；守恒 loads == resolved + notFound；零值行（新实例）；清单枚举路径（listFor）不计数（load-only 口径回归）。定向 `mvn -pl buzhou-skills test -Dtest='SkillResolutionStatsTest'` 绿 + SkillSearchToolTest 回归绿。
