---
id: T2887
title: 技能依赖图的形状裁决
type: task
status: closed
assignee: zcode-o
blocked-by: []
created: 2026-09-16
---

## Question]

技能「关系健康」怎么审计？（spec 1843 / effort #1843 / R44）

## Resolution

**npm/pip 依赖解析思想纯审计 `SkillDependencyAudit`（buzhou-skills）**：
Edge 契约（非空白/拒自指）+ audit 三病分诊（hasCycle+exampleCycle 显式栈
DFS 首环路径——确定性；missingDependencies 目标不在声明集；orphanSkills
无边关联）。MAX_NODES=10_000 保险丝；缺失目标不入图不污染环检测。

