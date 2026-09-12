---
id: T1029
title: 相似度阈值判定器验证
type: task
status: closed
assignee: zcode-g
blocked-by: [T1028]
created: 2026-09-12
---

## Question

相似度口径与边界如何证明？

## Resolution

**用户常设授权 AFK（可推翻）**

验证（G 会话第 15 轮 = effort #714）：①全等 1.0 过+detail 含分数；②无关 0 不过；③大小写/空白/词序微变高分过；④阈值边界含等号；⑤minRatio 越界拒绝+空串退化口径。buzhou-core 全模块零回归（C 会话排除集）。
