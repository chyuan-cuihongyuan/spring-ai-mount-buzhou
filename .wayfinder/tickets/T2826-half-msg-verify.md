---
id: T2826
title: 半消息审计的落地验证
type: task
status: closed
assignee: zcode-o
blocked-by: [T2825]
created: 2026-09-16
---

## Question

三态账目在账目/边界/哨兵/畸形四面下正确吗？（spec 1812 / effort #1812 / R13）

## Resolution

**HalfMessageAuditTest 4 用例全绿**（mvn -pl buzhou-core test
-Dtest=HalfMessageAuditTest）：三态+超阈+两比率；阈含边界且已裁决不受影响；
空表/null 哨兵；负阈/空 key/负年龄/null 状态 fail-fast。

