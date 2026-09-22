---
id: T3036
title: 重试风暴哨兵的落地验证
type: task
status: closed
assignee: zcode-o
blocked-by: [T3035]
created: 2026-09-23
---

## Question)

哨兵在占比/阈值/畸形下正确吗？（spec 1917 / effort #1917 / R118）

## Resolution`

**RetryStormSentinelTest 3 用例全绿**（mvn -pl buzhou-core test
-Dtest=RetryStormSentinelTest）：占比 0.3 与 0.6；判定两侧恰阈值
含上；畸形三型 fail-fast。
