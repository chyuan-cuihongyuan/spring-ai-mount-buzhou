---
id: T6152
title: S 会话 S26 Skip List 跳跃表的验证裁决
type: task
status: closed
assignee: zcode-s
blocked-by: [T6151]
created: 2026-09-24
---

## Question

S26 合同怎么逐一验绿？（spec 5025 / effort #5025 / S26）

## Resolution

**验证通过**：SkipListTest 四测全绿——固定种子 500 随机操作
vs TreeMap 圣像键序取值全等；同种子重放；upsert 覆盖；null
键值 fail-fast。
