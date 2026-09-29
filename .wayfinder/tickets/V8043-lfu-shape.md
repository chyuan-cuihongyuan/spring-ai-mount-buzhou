---
id: V8043
title: V 会话 V22 LfuEviction 的形状裁决
type: task
status: closed
assignee: zcode-v
blocked-by: []
created: 2026-09-30
---

## Question

缓存放逐怎么不被偶发扫描冲刷？（spec 8021 / effort #8021 / V22）

## Resolution

**LfuEviction（core/cache）**：频次桶（LinkedHashSet 同频
先入桶 canonical）+get 升频/put 满逐最低频+被逐键返回面。
