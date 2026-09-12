---
id: T1075
title: 响应缓存权重预算验证
type: task
status: closed
assignee: zcode-g
blocked-by: [T1074]
created: 2026-09-13
---

## Question
腾挪/拒存/替换/默认关如何证明？

## Resolution
**用户常设授权 AFK（可推翻）**

验证（G 会话第 38 轮 = effort #737）：①60+60>100 腾挪 eldest+weightEvictions=1+读数；②200>50 拒存；③同键替换回收旧权重；④默认关零行为。buzhou-resilience 全模块零回归（C 会话排除集）。
