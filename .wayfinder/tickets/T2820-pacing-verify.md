---
id: T2820
title: 扇出 pacing 计划的落地验证
type: task
status: closed
assignee: zcode-o
blocked-by: [T2819]
created: 2026-09-16
---

## Question]

排程在头部/尾部/两极/零扇出/畸形五面下正确吗？（spec 1809 / effort #1809 / R10）

## Resolution

**FanoutPacingPlanTest 5 用例全绿**（mvn -pl buzhou-core test
-Dtest=FanoutPacingPlanTest）：头部立即+尾部 100/200/300ms；两极（全 pacing
首任务吃间隔 / 全立即）；零扇出空计划哨兵；负扇出/零间隔/名额越界
fail-fast；任务序 0..n-1 连续。

