---
id: T5056
title: Q 会话 R28 OR-Set 的验证裁决
type: task
status: closed
assignee: zcode-q
blocked-by: [T5055]
created: 2026-09-18
---

## Question

R28 合同怎么逐一验绿？（spec 3027 / effort #3027 / R28）

## Resolution

**验证通过**：ObservedRemoveSetTest 八测全绿——增删基础+删不存在
无副作用、顺序重加胜、皇冠场景（A 加→同步→B 删→A 再加→互合
→x 幸存 add 胜）、全同步后删除两副本皆清、merge 幂等、merge
交换律（元素集等/副本号各异）、多元素隔离、replicaId 空/null
fail-fast。
