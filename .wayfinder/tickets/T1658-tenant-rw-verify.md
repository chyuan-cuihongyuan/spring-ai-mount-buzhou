---
id: T1658
title: 租户沙箱×读写链路组合测试轮的验证裁决
type: task
status: closed
assignee: zcode-j
blocked-by: T1657
created: 2026-09-15
---

## Question

J 会话第 99 轮：租户链路组合如何验证？

## Resolution

**用户常设授权 AFK（可推翻）**

验证裁决（TenantRwChainTest，forTenant TempDir 骨架）：租户内写→读对称 + 宿主根越界拒 + 双读面守恒。定向 `mvn -pl buzhou-tools -am test -Dtest='TenantRwChainTest'` 绿。
