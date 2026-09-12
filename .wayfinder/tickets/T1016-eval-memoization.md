---
id: T1016
title: 评估项结果记忆化的裁决
type: task
status: closed
assignee: zcode-g
blocked-by:
created: 2026-09-12
---

## Question

回归跑全量重烧模型调用——加项级记忆化吗？ERROR 缓不缓存？模型漂移谁管？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（G 会话第 9 轮 = effort #708 / spec 708 / impl 608）：opt-in `setMemoizationKey`（null=关）——sig=sha256(dataset|itemId|input|expected|key)，stateStore 落 eval.memo.*；命中 detail `[MEMO] ` 前缀+hits/misses 计数；ERROR 不缓存（瞬时故障不固化）；读写失败降级直跑。模型漂移刻意排除（归漂移基线族——诚实边界）。包装在 retry 外层预算内层（口径一致）。
