---
id: T2130
title: 拓扑深度/扇出/环防护的验证
type: task
status: closed
assignee: zcode-l
blocked-by: T2129
created: 2026-09-14
---

## Question

如何证明深度/扇出计量、孤儿口径与环防护？

## Resolution

**用户常设授权 AFK（可推翻）**

`SpanTreeTopologyTest` 五测全绿（`mvn -pl buzhou-core -am test`）：空集合哨兵；三层树精确断言（depth=3/fanout=4/直方 TOOL_CALL=3+降序）；孤儿计数（父缺失+不计深度）；**父引用环收敛**（互为父子不炸栈、环不计深）；多根+平名典序。
