# 732 — 事件 payload 大小审计

> 来源：G 会话第 33 轮 = effort #732（观测成本治理）/ [T1064](../../.wayfinder/tickets/T1064-event-payload-audit.md) / [T1065](../../.wayfinder/tickets/T1065-event-payload-audit-verify.md) / impl 632。

## Problem
事件 payload（Map）直接落观测库/外发 webhook——哪类事件在吃存储与带宽没有证据面；异常风暴常伴随巨型 payload（全量上下文被塞进事件）。

## Solution
`EventPayloadSizeAudit.analyze(List<EventRecord>)` 纯函数：payload Jackson 序列化字节（与落盘/外发口径一致）按类型聚合——rows（count/totalBytes/maxBytes，totalBytes 降序+type 字典序稳定）+totalBytes/serialized/skipped（序列化失败诚实计数）。

## Out of Scope
限幅动作（归 726 分布+外发限幅既有）；采样（归观测管线）。
