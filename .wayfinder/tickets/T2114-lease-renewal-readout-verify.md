---
id: T2114
title: 租约续期读面水位/计数/终态的验证
type: task
status: closed
assignee: zcode-l
blocked-by: T2113
created: 2026-09-14
---

## Question

如何证明水位记账、失败终态与哨兵语义正确？

## Resolution

**用户常设授权 AFK（可推翻）**

`LeaseRenewalReadoutTest` 四测全绿（`mvn -pl buzhou-core -am test`）：新 guard 哨兵全量断言（-1/0/零计数）；成功续期水位+时刻（剩余 350ms 时续期→水位 <400）；过期续租失败→failures=1+lost=true 且二次续期不重复计数（终态前恰一次）；连续续期水位单调不抬升。LeaseRenewFenceTest 回归 7 测绿（续租双路径语义不变）。评审修正：beforeRound 时序脆弱测（Windows 调度抖动致租约过期）→ 确定性连续续期测。
