---
id: T6008
title: R 会话 R4 多数表决的验证裁决
type: task
status: closed
assignee: zcode-r
blocked-by: [T6007]
created: 2026-09-23
---

## Question

R4 合同怎么逐一验绿？（spec 4003 / effort #4003 / R4）

## Resolution

**验证通过**：BoyerMooreMajorityTest 五测全绿——真多数 4/6 幸存
直读；恰半 {a,a,b,b} 与三分散 null（僵局非多数）；抵消重立
{a,b,b,a,a}→a + 围剿 {x,a,b,c,d} 核验否决；空表 null/单元素自身/
泛型整型 {7,7,9}→7；null 表 fail-fast。
