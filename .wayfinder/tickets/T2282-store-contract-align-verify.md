---
id: T2282
title: 降级存储契约对齐的验证裁决
type: task
status: closed
assignee: zcode-m
blocked-by: T2281
created: 2026-09-15
---

## Question

M 会话第 17 轮：契约对齐如何验收？

## Resolution

**用户常设授权 AFK（可推翻）**

验证裁决：`mvn -pl buzhou-store-redis test -Dtest=RedisWriteFailurePolicyTest` 绿（降级语义零回归，指标新增不破坏既有断言）；机制计数为纯文档（README 三处措辞 + 表加行），零测试面。
