---
id: T1002
title: 路由慢启动 yml 装配的验证
type: task
status: closed
assignee: zcode-g
blocked-by: T1001
created: 2026-09-13
---

## Question

slow-start 声明 → bean 装配且热重载走 ramp？缺省零变化？非法值 fail-fast？

## Resolution

**用户常设授权 AFK（可推翻）**

验证（G 会话第 26 轮）：① properties 绑定（30s/缺省/负值三态）；② bean 方法直调——slowStart 声明时热重载上调走 ramp（routes() 落 floor）、缺省时瞬时到位（RoutingSlowStartTest 已证语义，此处证装配接线）。`mvn -pl buzhou-resilience -am test` 全绿。
