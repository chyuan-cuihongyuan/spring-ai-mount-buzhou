---
id: T5019
title: Q 会话 R10 ARC 缓存的形状裁决
type: task
status: closed
assignee: zcode-q
blocked-by: []
created: 2026-09-18
---

## Question

缓存怎么在新近敏感与频率敏感间自适应？（spec 3009 / effort #3009 / R10）

## Resolution

**AdaptiveReplacementCache（core/cache，泛型 K/V）**：ARC 四链
T1/T2+B1/B2 幽灵+p 自适应（B1 命中调增/B2 命中调减）+REPLACE
（T1 超 p 逐 T1 入 B1 否则逐 T2 入 B2）+命中晋升 T2+四读数对账
面。扫描抗性（一次性键只污染 T1）与热键保护兼得。
