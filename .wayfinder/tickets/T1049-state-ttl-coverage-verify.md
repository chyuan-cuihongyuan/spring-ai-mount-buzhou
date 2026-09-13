---
id: T1049
title: 会话状态 TTL 覆盖审计验证
type: task
status: closed
assignee: zcode-g
blocked-by: [T1048]
created: 2026-09-13
---

## Question

覆盖率与 producer 归因如何证明？

## Resolution

**用户常设授权 AFK（可推翻）**

验证（G 会话第 25 轮 = effort #724）：①双 producer 混合 coverage 精确+行字典序+persistent 归因；②空 map 空真 1.0；③null fail-fast。buzhou-core 全模块零回归（C 会话排除集）。
