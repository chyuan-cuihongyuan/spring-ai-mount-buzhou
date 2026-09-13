---
id: T1505
title: 手动压缩操作分布读面的形态裁决
type: task
status: closed
assignee: zcode-j
blocked-by:
created: 2026-09-14
---

## Question

J 会话第 28 轮：手动压缩操作分布读面在本仓是否有缺口？形态如何裁决？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（J 会话第 28 轮 = effort #1027 / spec 1027 / impl 780）：缺口成立——ManualCompactor.compact 返回逐次 CompactResult（skipped/folded/error 字段俱全）但**无跨调用聚合**：手动压缩累计完成/跳过/失败几次、累计折入多少消息不可见——宿主侧运维（压缩是否频繁失败、白跑 skipped 比例）无水位。落点 buzhou-memory compact 包：实例级 attempts/completed/skipped/failed/foldedMessages 五计数（守恒 completed + skipped + failed == attempts）+ 嵌套 record `CompactOpStats` + `stats()` 快照。实例级；嵌套类型不动 API 快照；happy path 与幂等语义逐位不变。
