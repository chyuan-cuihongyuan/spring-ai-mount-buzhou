---
id: T2990
title: 尾时延放大读面的落地验证
type: task
status: closed
assignee: zcode-o
blocked-by: [T2989]
created: 2026-09-23
---

## Question)

幂次放大在经典/反解/恒等/畸形下正确吗？（spec 1894 / effort #1894 / R95）

## Resolution`

**TailAmplificationTest 4 用例全绿**（mvn -pl buzhou-core test
-Dtest=TailAmplificationTest）：0.99^100≈0.366；反解
0.99^(1/100)≈0.9999；N=1 恒等；互逆交叉验证与畸形三型 fail-fast。
