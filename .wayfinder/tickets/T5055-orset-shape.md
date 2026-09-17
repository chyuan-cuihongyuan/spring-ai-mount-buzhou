---
id: T5055
title: Q 会话 R28 OR-Set 的形状裁决
type: task
status: closed
assignee: zcode-q
blocked-by: []
created: 2026-09-18
---

## Question

多副本集合并发删加怎么免协调收敛？（spec 3027 / effort #3027 / R28）

## Resolution

**ObservedRemoveSet（core/concurrent，泛型）**：OR-Set——add 带
唯一标签（replica+seq），remove 只墓碑观察到的标签（并发新标签
幸存——add 胜显式语义），merge 双并集幂等交换。与 LWW 寄存器/
PN-Counter 成 CRDT 三形态（集域补位）。
