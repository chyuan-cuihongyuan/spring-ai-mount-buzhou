---
id: T6150
title: S 会话 S25 MemTable 内存表的验证裁决
type: task
status: closed
assignee: zcode-s
blocked-by: [T6149]
created: 2026-09-24
---

## Question

S25 合同怎么逐一验绿？（spec 5024 / effort #5024 / S25）

## Resolution

**验证通过**：MemTableTest 五测全绿——upsert 覆盖；满拒写不
覆盖已有；drain 字典序导出清空；isFull 读数；负容量/null
fail-fast + 确定性回放。
