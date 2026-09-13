---
id: T1071
title: 事件配对完整性审计验证
type: task
status: closed
assignee: zcode-g
blocked-by: [T1070]
created: 2026-09-13
---

## Question

配对/悬空/孤儿区分如何证明？

## Resolution

**用户常设授权 AFK（可推翻）**

验证（G 会话第 36 轮 = effort #735）：配对 1+悬空 sp2+孤儿 sp3 区分；HITL 规则复用；空表/null fail-fast。buzhou-core 全模块零回归（C 会话排除集）。
