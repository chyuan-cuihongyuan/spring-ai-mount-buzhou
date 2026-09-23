---
id: T6149
title: S 会话 S25 MemTable 内存表的形状裁决
type: task
status: closed
assignee: zcode-s
blocked-by: []
created: 2026-09-24
---

## Question

写路径内存缓冲怎么有序满即滚动？（spec 5024 / effort #5024 /
S25）

## Resolution

**MemTable（core/metrics）**：LSM memtable 思想——put upsert、
满拒写（滚动责任调用方）、drain 字典序整表导出并清空（交接
点确定性）；isFull/size 读数；畸形 fail-fast。
