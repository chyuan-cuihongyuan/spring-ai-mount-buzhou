---
id: T5052
title: Q 会话 R26 Rabin-Karp 的验证裁决
type: task
status: closed
assignee: zcode-q
blocked-by: [T5051]
created: 2026-09-18
---

## Question

R26 合同怎么逐一验绿？（spec 3025 / effort #3025 / R26）

## Resolution

**验证通过**：RabinKarpSearchTest 六测全绿——300 随机三方对拍
（JDK+Kmp+本件）、可重叠 findAll 两组、无匹配 −1/空/模式长于文、
空模式双约定、内容哈希位置无关+宿主嵌入命中、千级高重复滚动
压力（唯一尾窗+1998 重叠零假阳）。
