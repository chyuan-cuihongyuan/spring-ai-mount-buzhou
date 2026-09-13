---
id: T1074
title: 响应缓存权重预算的裁决
type: task
status: closed
assignee: zcode-g
blocked-by:
created: 2026-09-13
---

## Question

ResponseCacheStore 同受大响应挤占——701 权重预算对称落地吗？

## Resolution
**用户常设授权 AFK（可推翻）**

决策（G 会话第 38 轮 = effort #737 / spec 737 / impl 637）：构造器扩 maxWeightChars（0=关）——权重=响应字符数（estimateChars 同口径）；替换同键先回收旧权重；写入后腾挪 eldest；超预算拒存；weightEvictions 独立口径；TTL 即弃回收。yml 接线留后续。
