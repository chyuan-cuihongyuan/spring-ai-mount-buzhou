---
id: T5030
title: Q 会话 R15 KMP 搜索的验证裁决
type: task
status: closed
assignee: zcode-q
blocked-by: [T5029]
created: 2026-09-18
---

## Question

R15 合同怎么逐一验绿？（spec 3014 / effort #3014 / R15）

## Resolution

**验证通过**：KmpSearchTest 八测全绿——首配三手算（10/2/4）、
可重叠 findAll（[0,1,2] 与 [0,2,4,6]）、无匹配 −1/空、空模式
indexOf 0+findAll 拒、模式长于文本、CLRS lps [0,0,1,2,3,0,1]
手算+两组边例、200 随机对拍 JDK indexOf（二字母高重叠压力）、
每命中子串自证。
