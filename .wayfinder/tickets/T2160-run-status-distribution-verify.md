---
id: T2160
title: 状态直方与滞后窗口的验证
type: task
status: closed
assignee: zcode-l
blocked-by: T2159
created: 2026-09-14
---

## Question

如何证明状态直方、滞后窗口与 worst offenders 排序？

## Resolution

**用户常设授权 AFK（可推翻）**

`RunStatusDistributionTest` 五测全绿（`mvn -pl buzhou-core -am test`）：空输入哨兵；全枚举直方（INTERRUPTED 缺省 0 可见）；滞后=崩溃暴露窗口精确断言（9-5=4）；同滞后典序+零滞后不入榜；COMPLETED 零滞后不入 RUNNING 维。评审修正：RunStateSnapshot 实为 8 参（ownerId/updatedAt）——构造修正。
