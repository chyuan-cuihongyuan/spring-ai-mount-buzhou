---
id: T2122
title: 校验读数守恒与七桶分桶的验证
type: task
status: closed
assignee: zcode-l
blocked-by: T2121
created: 2026-09-14
---

## Question

如何证明守恒式、单源分桶与非互斥语义？

## Resolution

**用户常设授权 AFK（可推翻）**

`ToolArgsValidationStatsTest` 六测全绿（`mvn -pl buzhou-core -am test`）：守恒（3=2+1）；非 JSON 入参入桶；**非互斥实证**（{"days":"abc"} 一条同时触发 missingRequired+typeMismatch，rejected 只 +1——评审修正：初版输入 {"city":123} 有 city 不触发缺必填，构造错误）；enum/range 分桶；无 schema 不入账；reset 归零。测试前后归零防串扰（静态面纪律）。
