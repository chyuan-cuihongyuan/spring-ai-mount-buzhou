---
id: T6007
title: R 会话 R4 多数表决的形状裁决
type: task
status: closed
assignee: zcode-r
blocked-by: []
created: 2026-09-23
---

## Question

超半数元素怎么单遍常数内存找？（spec 4003 / effort #4003 / R4）

## Resolution

**BoyerMooreMajority（core/metrics，纯静态）**：MJRTY 1991——配对
抵消（同候选 +1/异候选 −1/归零换候选），真多数必幸存但幸存未必
多数——二次核验定夺；恰半僵局非多数；空表/无多数 null。与
MisraGries 成对（k=1 特例确定性精确版）。
