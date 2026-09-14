---
id: T2112
title: 查询页守卫钳制与游标可读化的验证
type: task
status: closed
assignee: zcode-l
blocked-by: T2111
created: 2026-09-14
---

## Question

如何证明钳制/归一/可读化且不破坏既有翻页？

## Resolution

**用户常设授权 AFK（可推翻）**

`ListSessionsPageGuardTest` 五测全绿（`mvn -pl buzhou-observe-dashboard -am test`）：size=100 万钳制（3 条全回语义不放大）、size=0/-7 归一 1（原异常路径）、非法游标两路径可读 IAE（原裸 NFE）、小页翻页语义回归（2+1 页推进/末页无游标）、filtered 路径常量对齐+诚实降级不变。既有 DashboardQueryServiceTest/FilteredSessionListingTest 回归 8 测绿。
