---
id: T2932
title: O 系 R66 对账轮的验证
type: task
status: closed
assignee: zcode-o
blocked-by: [T2931]
created: 2026-09-16
---

## Question]

全仓 clean verify BUILD SUCCESS 且三门全绿吗？（spec 1865 / effort #1865 / R66）

## Resolution

**mvn clean verify BUILD SUCCESS**（十一波连续）——首跑遇 K 会话在途
编辑窗口（StreamTest null vs 0 半成品），90s 收尾后复验一次过绿；
快照门五类型一致 + 对账门四断言绿（显式退出码）。
