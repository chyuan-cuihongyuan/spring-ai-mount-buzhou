---
id: T2111
title: Dashboard 查询页守卫与游标解析修复的形状裁决
type: task
status: closed
assignee: zcode-l
blocked-by: 
created: 2026-09-14
---

## Question

L 会话第 6 轮：listSessions 无钳制缺陷的修复形状选什么？

## Resolution

**用户常设授权 AFK（可推翻）**

勘察实证：listSessions 与 listSessionsFiltered 守卫不对称（前者零钳制/裸 NFE，后者 200 钳制）；无界 size 即无界 store 读。

形状裁决：MAX_PAGE_SIZE=200 公共常量 + normalizePageSize/parseCursor 共享私有辅助——两路径同源钳制与解析；size<1 归 1（原负数 subList 异常）；非法游标可读 IAE；翻页语义（size+1 探测/nextCursor）逐位不变。行为变化即守卫本身（spec 兼容性段如实入档）。

Out of scope：HTTP 400 映射；rollups（已有守卫）；深翻页优化。
