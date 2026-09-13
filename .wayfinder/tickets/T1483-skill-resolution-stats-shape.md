---
id: T1483
title: 技能解析未命中计数读面的形态裁决
type: task
status: closed
assignee: zcode-j
blocked-by:
created: 2026-09-14
---

## Question

J 会话第 17 轮：技能解析未命中计数读面（幻觉技能名探测）在本仓是否有缺口？形态如何裁决？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（J 会话第 17 轮 = effort #1016 / spec 1016 / impl 769）：缺口成立——DefaultSkillRegistry.load 是模型面技能调用入口，解析为空（classpath+DB 双缺 = 技能不存在）即**模型幻觉技能名**的直读信号，但零计数不可见。与 I 池「技能使用统计（npm downloads）」分轴：那轴是命中分布（哪些技能热），本轴是**未命中探测**（模型在调用不存在的技能）。落点 buzhou-skills：DefaultSkillRegistry 实例级 loads/resolved/notFound 三 AtomicLong——**只在 load() 计数**（模型面入口；listFor/listAllFor/isVisibleFor 是清单枚举路径不计，避免污染幻觉信号），守恒 loads == resolved + notFound；新公共 record `SkillResolutionStats` + `resolutionStats()`。
