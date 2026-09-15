---
id: T2890
title: 重连退避阶梯的落地验证
type: task
status: closed
assignee: zcode-o
blocked-by: [T2889]
created: 2026-09-16
---

## Question]

阶梯在爬升/封顶/放弃/畸形四面下正确吗？（spec 1844 / effort #1844 / R45）

## Resolution

**ReconnectBackoffLadderTest 4 用例全绿**（mvn -pl buzhou-mcp test
-Dtest=ReconnectBackoffLadderTest）：100×2 → 100/200/400/800；cap 300
第 3 次钳 300+attempt 1000 仍 300+MAX_VALUE 溢出安全；verdict(5,5)=RETRY
/(6,5)=GIVE_UP；零尝试/base<1/cap<base fail-fast。

