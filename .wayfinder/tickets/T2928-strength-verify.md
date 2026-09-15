---
id: T2928
title: 凭据强度计的落地验证
type: task
status: closed
assignee: zcode-o
blocked-by: [T2927]
created: 2026-09-16
---

## Question]

强度计在类别/阶梯/边界/畸形四面下正确吗？（spec 1863 / effort #1863 / R64）

## Resolution`

**CredentialStrengthMeterTest 4 用例全绿**（mvn -pl buzhou-guard test
-Dtest=CredentialStrengthMeterTest）：四类/单类/双类计数；短杂/单类
WEAK、12 位三类 FAIR、16 位三类 STRONG；边界含上（12→FAIR、16→STRONG
——字符串长度断言拆开）；空白/null fail-fast。首跑编译红（枚举返回值
误用 hasSize）修正。

