---
id: T3050
title: 空闲连接收割的落地验证
type: task
status: closed
assignee: zcode-o
blocked-by: [T3049]
created: 2026-09-23
---

## Question)

收割在边界/排序/畸形下正确吗？（spec 1924 / effort #1924 / R125）

## Resolution`

**IdleConnectionReaperTest 3 用例全绿**（mvn -pl buzhou-core test
-Dtest=IdleConnectionReaperTest）：混合闲置入选/不入选/恰边界含
上；闲置最久排前；畸形三型 fail-fast。
