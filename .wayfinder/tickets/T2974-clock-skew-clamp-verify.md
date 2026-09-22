---
id: T2974
title: 时钟偏斜校正的落地验证
type: task
status: closed
assignee: zcode-o
blocked-by: [T2973]
created: 2026-09-23
---

## Question)

钳位在平移/内嵌/收缩/串联/畸形五面下正确吗？（spec 1886 / effort #1886 / R87）

## Resolution`

**ClockSkewClampTest 5 用例全绿**（mvn -pl buzhou-core test
-Dtest=ClockSkewClampTest）：负偏斜 1000-1100/父 1050 → 1050-1150
时长保持；正常内嵌零改动；终点越界收缩；双越界 900-1050/父
1000-1100 → 1000-1100 串联；畸形 fail-fast。
