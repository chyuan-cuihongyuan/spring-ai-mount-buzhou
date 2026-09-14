---
id: T2329
title: F8/F11 判定收尾的形状裁决
type: task
status: closed
assignee: zcode-m
blocked-by: T2327
created: 2026-09-15
---

## Question

M 会话第 43 轮：design-incompleteness F8（标签装配入口）与 F11（Spill 终检）如何裁定？

## Resolution

**用户常设授权 AFK（可推翻）**

① F8：编程面 only 定案——SessionIndexObserver.wiring() 公开构造可传标签 map（既有公共面够用）；yml 装配不开（标签语义业务自定，配置面无默认值可兜）；② F11：单路径 Hook 化定案——CopyOnWriteGuardHook/OnloadHook 已覆盖进出两向（Hook 层唯一落盘点），无双路径即无幂等问题，manager 终检形态不采用（spec 05 推演段回写）。F1-F11 全档闭环。
