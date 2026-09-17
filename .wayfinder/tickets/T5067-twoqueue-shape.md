---
id: T5067
title: Q 会话 R34 2Q 缓存的形状裁决
type: task
status: closed
assignee: zcode-q
blocked-by: []
created: 2026-09-18
---

## Question

扫描抗性怎么比 ARC 更轻地拿到？（spec 3033 / effort #3033 / R34）

## Resolution

**TwoQueueCache（core/cache，泛型）**：2Q 简化版——入口 A1in FIFO
观察窗+主区 Am LRU，二触晋升（「热不热看第二触」分诊）；一次性
扫描流只在观察窗自旋。静态比分简于 ARC 幽灵自适应——与 ARC/
CLOCK 成驱逐三档（自适应/静态分诊/零分诊）。
