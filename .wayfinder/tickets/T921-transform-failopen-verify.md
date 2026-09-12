---
id: T921
title: fail-open 观测验证口径
type: task
status: closed
assignee: zcode-f
blocked-by: T920
created: 2026-09-12
---

## Question

观测语义如何钉住？

## Resolution

**用户常设授权 AFK（可推翻）**

验证口径（TransformingFailOpenObservabilityTest 2/2 + core 全模块 1785/1785 零回归）：

- 抛异常两连击：计数 2、原文照返；null/空白各计数 1；成功路径 0。
- 空原文短路不算失败；wrap null 校验不变。
