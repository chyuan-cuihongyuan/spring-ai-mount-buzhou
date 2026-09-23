---
id: T6138
title: S 会话 S19 Read Repair 读修复的验证裁决
type: task
status: closed
assignee: zcode-s
blocked-by: [T6137]
created: 2026-09-24
---

## Question

S19 合同怎么逐一验绿？（spec 5017a / effort #5018 / S19）

## Resolution

**验证通过**：ReadRepairTest 五测全绿——一致汇报空 stale；
分叉 max 版本胜 + stale 字典序；并列 tie-break；空汇报/负
版本/null fail-fast；确定性回放。
