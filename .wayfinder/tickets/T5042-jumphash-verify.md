---
id: T5042
title: Q 会话 R21 跳增哈希的验证裁决
type: task
status: closed
assignee: zcode-q
blocked-by: [T5041]
created: 2026-09-18
---

## Question

R21 合同怎么逐一验绿？（spec 3020 / effort #3020 / R21）

## Resolution

**验证通过**：JumpConsistentHashTest 七测全绿——万键值域夹持、
10 万键 10 桶均匀 ±0.01、10→11 扩容迁移率 1/11±0.01 且迁移键
全落新桶（最小迁移定量证据）、确定性（含负键/极值）、单桶恒 0、
负键全域、0/负桶数 fail-fast。
