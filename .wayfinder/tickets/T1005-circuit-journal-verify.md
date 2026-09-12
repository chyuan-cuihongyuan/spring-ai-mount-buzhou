---
id: T1005
title: 断路器变迁事件流读数验证
type: task
status: closed
assignee: zcode-g
blocked-by: [T1004]
created: 2026-09-12
---

## Question

journal 与真状态机的对齐如何证明？

## Resolution

**用户常设授权 AFK（可推翻）**

验证（G 会话第 3 轮 = effort #702）：用真状态机驱动（双失败→OPEN/快进时钟→HALF_OPEN/探测达标→CLOSED）断言 journal 三条变迁与聚合；环形覆盖 dropped；snapshot 防御拷贝+null fail-fast。buzhou-resilience 全模块零回归（C 会话排除集）。
