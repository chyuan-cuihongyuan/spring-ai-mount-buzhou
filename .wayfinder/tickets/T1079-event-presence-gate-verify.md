---
id: T1079
title: 事件静默缺失门验证
type: task
status: closed
assignee: zcode-g
blocked-by: [T1078]
created: 2026-09-13
---

## Question
缺失检测如何证明？

## Resolution
**用户常设授权 AFK（可推翻）**

验证（G 会话第 40 轮 = effort #739）：缺失清单+全满足+空契约不误报+null fail-fast。buzhou-core 全模块零回归（C 会话排除集）。
