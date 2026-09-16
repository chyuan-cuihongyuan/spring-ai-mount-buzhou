---
id: T3212
title: 有界 Top-K 收集器的验证裁决
type: task
status: closed
assignee: zcode-p
blocked-by: [T3211]
created: 2026-09-17
---

## Question

BoundedTopK 合同（逐换/同分保位/门槛/有界/畸形）怎么钉住？（spec 2055 / effort #2055 / R56）

## Resolution

**七用例全绿**（首跑 1 红教训：递增流每值胜守门员——全程逐换零落选，
rejected/evicted 期望与流形态相关，修断言后 7/7）：降序前三 9/7/5
落选 1 / 胜守门员逐换 evicted 1 / 同分先入者保位 / 未满全收门槛=
最小 / 空榜 −∞ / 万级递增恰留 top5（rejected=0 evicted=9995） /
畸形三型（容量 0、null item、NaN score）fail-fast。
