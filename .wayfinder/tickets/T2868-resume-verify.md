---
id: T2868
title: 续读令牌的落地验证
type: task
status: closed
assignee: zcode-o
blocked-by: [T2867]
created: 2026-09-16
---

## Question]

令牌在回路/三态/畸形七面下正确吗？（spec 1833 / effort #1833 / R34）

## Resolution

**ResumeTokenCodecTest 4 用例全绿**（mvn -pl buzhou-core test
-Dtest=ResumeTokenCodecTest）：编解码回路（含指纹内含 @）；三态+边界
（==max VALID、换代+越界并存取 STALE）；畸形令牌四型 fail-fast；畸形
入参三型 fail-fast。

