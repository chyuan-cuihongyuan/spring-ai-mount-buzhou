---
id: T1321
title: SessionIndexStore 契约校验套件的形态裁决
type: task
status: closed
assignee: zcode-i
blocked-by:
created: 2026-09-14
---

## Question

I 会话第 45 轮：SessionIndexStore（会话治理第一查询面：upsert/get/list/delete/purgeOlderThan）的契约套件是否有缺口？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（I 会话第 45 轮 = effort #945 / spec 945 / impl 694）：扩散成立（922/936 系列延续）。落点 `SessionIndexStoreContract`（spi 静态 verify 范式）五项检查——①upsert→get 往返一致 ②重复 upsert 同 id 覆盖幂等 ③delete 后 get empty 且 delete 幂等 ④list 默认排除 DELETED 状态（spec 33 §B 审计行显式过滤口径）⑤purgeOlderThan 按 lastActive 截止删除并返回计数、limit 尊重。配套内存实现接入测试（core 测试域）。
