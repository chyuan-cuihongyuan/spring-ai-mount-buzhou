---
id: T1121
title: 存储延迟环形读数的形态裁决
type: task
status: closed
assignee: zcode-h
blocked-by: []
created: 2026-09-13
---

## Question

存储延迟样本环放哪层、喂点怎么给、异常路径记不记？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（H 会话第 11 轮 = effort #810 / spec 810 / impl 563）：`StoreLatencyRing`（per-op FIFO 128+最近秩 P50/P95+操作名封顶 16）+ `TimedMessageStore` 装饰器（nanoTime finally 计时——异常照记照抛）；其余 store 域可复用环手动喂。
