---
id: T1103
title: Redis 大值审计的形态裁决
type: task
status: closed
assignee: zcode-h
blocked-by: []
created: 2026-09-13
---

## Question

大值（BIGKEY）审计做连接型还是纯函数型？族归类与定级档位如何定？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（H 会话第 2 轮 = effort #801 / spec 801 / impl 554）：`RedisValueSizeAudit` 纯函数——采样与判定解耦（705 口径）；九族前缀归类+other 不猜测；WARN/CRIT=1×/2× 阈值两档；Top32+按族聚合+治理提示；脏样本跳过；阈值 <1 fail-fast。
