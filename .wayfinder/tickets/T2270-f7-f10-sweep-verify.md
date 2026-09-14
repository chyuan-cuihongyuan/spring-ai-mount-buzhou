---
id: T2270
title: design-incompleteness 小缺口清扫的验证裁决
type: task
status: closed
assignee: zcode-m
blocked-by: T2269
created: 2026-09-15
---

## Question

M 会话第 10 轮：F7/F10 清扫如何验收？

## Resolution

**用户常设授权 AFK（可推翻）**

验证裁决：`mvn -pl buzhou-resilience test -Dtest=CanaryRoutingEndToEndTest` 绿——
① canary.selected payload 含 sessionId（等于会话 id）与 model（F7 断言钉住）；
② 既有 canary 测试（每会话一次/默认关零事件/粘住性）零回归；
③ spec 07 双处回写 + design-incompleteness F7/F10 闭环标记。
