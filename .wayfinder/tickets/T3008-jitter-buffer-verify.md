---
id: T3008
title: 自适应抖动缓冲的落地验证
type: task
status: closed
assignee: zcode-o
blocked-by: [T3007]
created: 2026-09-23
---

## Question)

覆盖分位与覆盖率在极值/畸形下正确吗？（spec 1903 / effort #1903 / R104）

## Resolution`

**JitterBufferTest 4 用例全绿**（mvn -pl buzhou-core test
-Dtest=JitterBufferTest）：样本 {10..50} target 0.9→50ms 0.5→30ms；
覆盖率 30→0.6 与 50→1.0；target=1.0 极值；畸形三型 fail-fast。
