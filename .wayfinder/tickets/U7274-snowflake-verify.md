---
id: U7274
title: U 会话 U37 SnowflakeIdGenerator 的验证裁决
type: task
status: closed
assignee: zcode-u
blocked-by: [U7273]
created: 2026-09-29
---

## Question

U37 合同怎么逐一验绿？（spec 7036 / effort #7036 / U37）

## Resolution

**验证通过**：四测全绿——批量唯一+严格递增（勘误：固定时钟自旋永真改步进）；分解往返；回退 fail-fast；隔离。
