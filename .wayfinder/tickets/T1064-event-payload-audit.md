---
id: T1064
title: 事件 payload 大小审计的裁决
type: task
status: closed
assignee: zcode-g
blocked-by:
created: 2026-09-13
---

## Question
payload 体量无治理证据面——加大小审计吗？

## Resolution
**用户常设授权 AFK（可推翻）**

决策（G 会话第 33 轮 = effort #732 / spec 732 / impl 632）：`EventPayloadSizeAudit.analyze` 纯函数——Jackson 序列化字节按类型聚合（count/total/max，totalBytes 降序）+serialized/skipped 诚实计数。纯读数。
