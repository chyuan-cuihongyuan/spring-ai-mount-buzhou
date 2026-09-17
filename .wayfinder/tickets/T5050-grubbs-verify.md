---
id: T5050
title: Q 会话 R25 Grubbs 检验的验证裁决
type: task
status: closed
assignee: zcode-q
blocked-by: [T5049]
created: 2026-09-18
---

## Question

R25 合同怎么逐一验绿？（spec 3024 / effort #3024 / R25）

## Resolution

**验证通过**：GrubbsOutlierTest 七测全绿——高/低双侧离群检出、
干净六点不误报（outlier null）、{1,2,2,3,4,100} 手算对齐（实际
{1,2,3,4,100}: mean=22/s=√1902.5/G≈1.7879 超 1.672）、零方差
拒绝、三路表界 fail-fast、临界随 n 严格增。首版表长 30 vs 断言
28 不自洽（列 30 值覆盖 n=3..32）——MAX 由表长+MIN 常量导出
根治，手抄表长不再双写教训入档。
