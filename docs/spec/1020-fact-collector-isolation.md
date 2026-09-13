# 1020 — 事实采集隔离硬化与计数读面

> 来源：J 会话第 21 轮 = effort #1020（[T1491](../../.wayfinder/tickets/T1491-fact-collector-isolation-shape.md) / [T1492](../../.wayfinder/tickets/T1492-fact-collector-isolation-verify.md) / impl 773）。范式先例：spec 13 §core-1「事件分发逐监听器隔离」同款（本仓自家惯例推广到采集器族）。

## Problem Statement

`FactCollectorHook.afterTool` 顺序遍历 `FactDefinition` 判定并落库，但 judge() 与 save() **无任何隔离**：单定义判定器抛异常（业务判定器自带解析逻辑，输入形状不可控）→ 异常上传播炸整条 afterTool 链——其余定义全部漏采、后续 hook（order > 200）全部跳过。一个坏定义拖垮整条工具后处理管线。

## 目标

- 逐定义隔离：judge 异常 / save 失败各自捕获入 `failures` 桶，**其余定义照常采集**、链继续（对齐 spec 13 监听器隔离惯例）。
- 实例级计数 `saved` / `failures` 两 AtomicLong + 嵌套 record `FactCollectionStats(saved, failures)` + `stats()` 快照。

## 兼容性

**语义变化点（本轮正主题）**：定义级 judge/save 异常不再向上传播（原先炸链）——happy path 逐位不变；对齐既有隔离惯例。无新配置项。

## Out of Scope

- 失败升级事件/告警（failures 计数已给装配层自警依据）。
- 按定义分桶计数（定义数通常个位数，总桶已足）。
