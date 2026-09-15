---
id: T2833
title: 负载脱落阶梯的形状裁决
type: task
status: closed
assignee: zcode-o
blocked-by: []
created: 2026-09-16
---

## Question

过载处置的优先级语义怎么安放？（spec 1816 / effort #1816 / R17）

## Resolution

**Envoy overload manager 思想纯裁决 `LoadShedLadder`（core/backpressure）**：
Level(name, threshold) 阶梯级（低阈值=低优先级先掉，契约构造 fail-fast）；
decide(loadFactor, ladder) → 越阈即甩（含边界）ShedDecision（shedLevels/
keptLevels + shedRatio -1 哨兵 + escalating() 升级信号）。与
PrefetchCreditWindow（入口闸）互补：过载后分级甩。

