---
id: T3044
title: 时钟抖动测量的落地验证
type: task
status: closed
assignee: zcode-o
blocked-by: [T3043]
created: 2026-09-23
---

## Question)

抖动读数在恒定/已知/滚动/畸形下正确吗？（spec 1921 / effort #1921 / R122）

## Resolution`

**ClockJitterMeterTest 4 用例全绿**（mvn -pl buzhou-core test
-Dtest=ClockJitterMeterTest）：恒定偏斜抖动 0；已知样本集抖动精确
断言；均值符号正确；不足样本哨兵 -1.0；窗口滚动覆盖最旧；窗口<2
fail-fast。
