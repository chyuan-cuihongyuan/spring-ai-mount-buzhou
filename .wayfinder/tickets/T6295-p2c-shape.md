---
id: T6295
title: T 会话 T47 PowerOfTwoChoices 二择一负载均衡的形状裁决
type: task
status: closed
assignee: zcode-t
blocked-by: []
created: 2026-09-28
---

## Question

长尾热桶怎么 O(1) 放置压平？（spec 6047 / effort #6047 / T47）

## Resolution

**PowerOfTwoChoices（core/policy，Wave 7 遗珠）**：
随机抽两个不同桶（同桶重抽）取较轻（并列取小下标确定）；
seeded Random 可回放；placed/loadOf/maxLoad 读数；越域
fail-fast。
