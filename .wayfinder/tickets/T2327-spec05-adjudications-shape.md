---
id: T2327
title: spec 05 判定项批量回写（F3/F4/F6）的形状裁决
type: task
status: closed
assignee: zcode-m
blocked-by:
created: 2026-09-15
---

## Question

M 会话第 42 轮：design-incompleteness F3（advisor 注入通道）、F4（键表漂移）、F6（随机源类型）如何裁定？

## Resolution

**用户常设授权 AFK（可推翻）**

① F3：实现定案 per-session 组装（BoundedToolCallingAdvisor 随会话构造，机制模块经 customizer 注入）——原设计 Builder Bean 通道不采用（per-session 与租约/隔离舱会话级机制对齐），spec 05 回写；
② F4：`buzhou.parallel.*` 键族全仓未实现——键表按实现重写（真键四行 + config-reference 指针；并发 8 为 HarnessAssembler 编程式默认入档）；
③ F6：退避抖动为 DoubleSupplier 纯函数（backoffMillis(failures)）——spec 回写实现口径，注入面不补（纯函数无随机源注入必要）。
