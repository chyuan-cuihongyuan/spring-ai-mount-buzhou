# 1016 — 技能解析未命中计数读面

> 来源：J 会话第 17 轮 = effort #1016（[T1483](../../.wayfinder/tickets/T1483-skill-resolution-stats-shape.md) / [T1484](../../.wayfinder/tickets/T1484-skill-resolution-stats-verify.md) / impl 769）。借鉴：Berkeley [function-calling leaderboard](https://gorilla.cs.berkeley.edu/leaderboard.html)——模型幻觉函数/工具名的生产探测。

## Problem Statement

`DefaultSkillRegistry.load` 是模型面技能调用入口：名字解析为空（classpath+DB 双缺）= 模型在调用**不存在的技能**（幻觉技能名）。现零计数——幻觉率不可见，提示词里技能清单的呈现质量（是否诱发了幻觉）无从评估。

## 目标

- `DefaultSkillRegistry` 增量（buzhou-skills，实例级）：`loads` / `resolved` / `notFound` 三 AtomicLong，守恒不变量 **loads == resolved + notFound**。
- **只在 `load()` 计数**（模型面入口）——listFor/listAllFor/isVisibleFor 是清单枚举路径不计数（避免渲染流量污染幻觉信号）。
- 新公共 record `SkillResolutionStats(long loads, long resolved, long notFound)`（skill 包，api 面）+ `resolutionStats()` 快照。

## 兼容性

纯增量读面：load/listFor/listAllFor/isVisibleFor 语义逐位不变；无新配置项。

## Out of Scope

- 按技能名分桶的命中分布（I 会话「技能使用统计」轴，回避）。
- 幻觉名升级 WARN/事件（语义另议）。
