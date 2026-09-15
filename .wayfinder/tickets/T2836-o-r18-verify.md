---
id: T2836
title: O 系 R18 对账轮的验证
type: task
status: closed
assignee: zcode-o
blocked-by: [T2835]
created: 2026-09-16
---

## Question

全仓 clean verify BUILD SUCCESS 且三门全绿吗？（spec 1817 / effort #1817 / R18）

## Resolution

**clean verify 全模块绿、仅 starter 双门因漏登 spec 1817 README 行红**
（OSession1800LedgerAuditTest+SpecCoverageTest 同根因——对账门当场抓漏，
设计生效）；补行后复跑双门绿。其余 16 模块在同一 clean 构建内全绿，
仅做 starter 段复跑的增量验证。
