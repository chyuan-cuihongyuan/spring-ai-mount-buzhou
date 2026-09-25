---
id: T6206
title: T 会话 T3 Treap 树堆的验证裁决
type: task
status: closed
assignee: zcode-t
blocked-by: [T6205]
created: 2026-09-25
---

## Question

T3 合同怎么逐一验绿？（spec 6002 / effort #6002 / T3）

## Resolution

**验证通过**：TreapTest 五测全绿——500 键顺序插入 height
≤32+中序+TreeMap 圣像取值全等；扰动 500 步 oracle 全等；
同种子双实例同构/异种子根异；fail-fast。
