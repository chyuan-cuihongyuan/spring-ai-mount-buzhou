---
id: T3181
title: 可冻结分段缓冲的形状裁决
type: task
status: closed
assignee: zcode-p
blocked-by: []
created: 2026-09-17
---

## Question

spill 缓冲的并发快照安全与整段 flush 怎么兼得？（spec 2040 / effort #2040 / R41）

## Resolution

**LSM memtable 思想 `FreezableBuffer<T>`（buzhou-spill，模块首入 P 系）**：
可变段满自动封冻为不可变段（冻结后只读——并发快照安全）+手动 freeze
（空段 no-op）+drainFrozen 整段取走（flush 语义——写放大的批量化）+
snapshot 冻结序+可变尾追加序稳定+frozenCount 积压面。
