---
id: T6044
title: R 会话 R22 工作窃取的验证裁决
type: task
status: closed
assignee: zcode-r
blocked-by: [T6043]
created: 2026-09-23
---

## Question

R22 合同怎么逐一验绿？（spec 4021 / effort #4021 / R22）

## Resolution

**验证通过**：WorkStealingSplitTest 五测全绿——定量表（0→0、
1/2/3→1、4→2、10→5、11→5）；五元队冷端偷 [a,b] 热端 e 不动；
百项连续三偷 50/25/12 梯度摊平；单元素全偷/空队空偷；畸形三型
fail-fast（0 下限/负 size/null）。
