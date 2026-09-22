---
id: T2982
title: 阻塞期审计的落地验证
type: task
status: closed
assignee: zcode-o
blocked-by: [T2981]
created: 2026-09-23
---

## Question)

分诊在三类主导/并列/守恒破坏下正确吗？（spec 1890 / effort #1890 / R91）

## Resolution`

**WaitProfileAuditTest 5 用例全绿**（mvn -pl buzhou-core test
-Dtest=WaitProfileAuditTest）：锁主导（2/20/8/30→LOCK_WAIT 比例
0.93）/IO 主导/CPU 主导；并列固定序；守恒破坏两例 fail-fast；
阻塞比精确。
