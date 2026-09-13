---
id: T1009
title: 提示词角色构成拆解读数验证
type: task
status: closed
assignee: zcode-g
blocked-by: [T1008]
created: 2026-09-12
---

## Question

拆解正确性与口径边界如何证明？

## Resolution

**用户常设授权 AFK（可推翻）**

验证（G 会话第 5 轮 = effort #704）：①三角色构成降序+share 精确（0.5/0.3/0.2）+messages 计数；②空 prompt/纯 null 文本 → total=0 share 全 0 不 NaN；③null fail-fast+同值字典序稳定。buzhou-core 全模块零回归（C 会话排除集）。
