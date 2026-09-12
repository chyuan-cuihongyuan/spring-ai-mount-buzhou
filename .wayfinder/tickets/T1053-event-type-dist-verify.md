---
id: T1053
title: 事件类型分布读数验证
type: task
status: closed
assignee: zcode-g
blocked-by: [T1052]
created: 2026-09-13
---

## Question

聚合口径如何证明？

## Resolution

**用户常设授权 AFK（可推翻）**

验证（G 会话第 27 轮 = effort #726）：计数降序+字典序稳定+topType 占比/空表 total=0 topType null/null fail-fast。buzhou-core 全模块零回归（C 会话排除集）。
