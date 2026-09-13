---
id: T1491
title: 事实采集隔离硬化与计数读面的形态裁决
type: task
status: closed
assignee: zcode-j
blocked-by:
created: 2026-09-14
---

## Question

J 会话第 21 轮：事实采集 judge 隔离硬化与计数读面在本仓是否有缺口？形态如何裁决？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（J 会话第 21 轮 = effort #1020 / spec 1020 / impl 773）：缺口成立且属**行为缺陷级**——FactCollectorHook.afterTool 对 judge()/save() 无任何隔离：单定义判定器抛异常会炸掉整条 afterTool 链（其余定义、后续 hook 全部跳过——违背 spec 13「逐监听器隔离」本仓惯例）。落点 buzhou-guard fact 包：逐定义 try/catch 隔离（judge 异常/save 失败各自入 failures 桶，其余定义照常采集）+ 实例级 saved/failures 两 AtomicLong + 嵌套 record `FactCollectionStats(saved, failures)` + `stats()`。隔离语义变化 = 本轮正主题（对齐 deliverEvent/监听器隔离先例）， happy path 零变化。
