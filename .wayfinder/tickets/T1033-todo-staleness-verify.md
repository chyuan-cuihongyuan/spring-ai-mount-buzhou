---
id: T1033
title: Todo 陈旧度审计读数验证
type: task
status: closed
assignee: zcode-g
blocked-by: [T1032]
created: 2026-09-12
---

## Question

滞留判定与 promptHint 形态如何证明？

## Resolution

**用户常设授权 AFK（可推翻）**

验证（G 会话第 17 轮 = effort #716）：①混合状态滞留集合精确+rows 降序+byStatus；②completed 不计滞留；③阈值 0 关闭；④promptHint 形态（有滞留/无滞留空串）；⑤null fail-fast。buzhou-tools 全模块零回归（C 会话排除集）。
