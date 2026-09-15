# impl 1444 — SkillDependencyAudit 技能依赖图审计（R44 = effort #1843 / spec 1843 / T2887-T2888）

**What**：`SkillDependencyAudit`（buzhou-skills 静态纯函数）——audit 三病
分诊（hasCycle+exampleCycle 显式栈 DFS/missingDependencies/orphanSkills）
+ MAX_NODES 保险丝；空白边/自指边 fail-fast。

**Why**：npm/pip 依赖解析思想——循环依赖加载序无解、悬空引用装不齐、
孤儿是目录噪音；三病各需各的处方（重构解环/补装/清理），装得齐≠依赖
健康。

**Verify**：`SkillDependencyAuditTest` 5 用例全绿（初版漏标起点 visited
缺陷自查修正）。

**Status**：done（2026-09-16）
