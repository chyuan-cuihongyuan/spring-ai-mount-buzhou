# Spec 1843 — 技能依赖图审计（effort #1843，R44）

> wayfinder map：`.wayfinder/maps/effort-1800.md`（T2887–T2888，impl 1444）。借鉴：
> npm/pip 依赖解析——环（加载序无解）/缺失依赖（悬空引用）/孤儿（目录
> 噪音）三病分诊，装得齐≠依赖健康。

## Problem Statement

技能目录只验证「存在的都合法」，不验「关系是否健康」：循环依赖让加载
拓扑无解、边指向未声明技能是悬空引用、无边孤儿是目录噪音——三病无审计
面则各需的处方（重构解环/补装/清理）无从触发。

## Solution

`SkillDependencyAudit`（buzhou-skills，静态纯函数）：

- `Edge(skill, dependsOn)` 契约构造（两端非空白、自指拒绝）；
- `audit(skillNames, edges)` → `Audit(skills, edges, hasCycle, exampleCycle,
  missingDependencies, orphanSkills)`：显式栈 DFS 找环（首环路径入档，
  确定性入参序）；缺失依赖（目标不在声明集）与孤儿（无边关联声明技能）
  分账；MAX_NODES=10_000 保险丝。

## User Stories

1. 作为技能目录维护者，hasCycle=true + exampleCycle=[a,b,c,a] → 环路径
   直接指出该断哪条边。
2. 作为安装治理者，missingDependencies=1 → 有技能声明了依赖却没装齐——
   补装或降级。
3. 作为目录清理者，orphanSkills 常年非零 → 死技能该清（没人依赖也不
   依赖人）。

## Implementation Decisions

- 纯审计不解析（加载拓扑归宿主）；显式栈 DFS 免深递归溢出；缺失目标
  不入图（另有账，不污染环检测）。
- 初版漏标起点 visited（跨起点重访缺陷）自查修正入档。

## Testing Decisions

- 健康 DAG 干净账；环检测+路径首尾同点；缺失/孤儿分账；空输入哨兵；
  畸形边 fail-fast。

## Out of Scope

- 不做拓扑排序输出；不做版本范围解析（semver 归未来静脉）。

## Further Notes

- 与 SkillCatalogDriftWatcher 正交：那是目录漂移，这是依赖关系健康。
