---
id: T6238
title: T 会话 T19 KD-Tree 二维最近邻的验证裁决
type: task
status: closed
assignee: zcode-t
blocked-by: [T6237]
created: 2026-09-26
---

## Question

T19 合同怎么逐一验绿？（spec 6018 / effort #6018 / T19）

## Resolution

**验证通过**：KdTreeTest 五测全绿——200 点×100 查询暴力
圣像（含平局规则）；共线退化；重复点平局；负坐标；
fail-fast（源码 T18 批预入档，验证随 T18 verify 三门绿）。
