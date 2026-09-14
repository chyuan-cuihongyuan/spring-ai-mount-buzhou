# 1087 — memory 域双工具组合测试轮

> 来源：J 会话第 87 轮 = effort #1087（[T1629](../../.wayfinder/tickets/T1629-memorytools-shape.md) / [T1630](../../.wayfinder/tickets/T1630-memorytools-verify.md) / impl 839）。纯测试轮第五弹（R81/R82/R84/R86 先例）。

## Problem Statement

R59 CompactNowTool（手动压缩）与 R55 EpisodeLedger（情景记忆）双读面同域协同——**独立性与一致性**无验证：串账或 reset 互相污染破坏两读面独立对账。

## 目标

新增 `MemoryToolsReadoutTest`（buzhou-memory）：compact_now 调用与 EpisodeLedger record/recall 交叉后，双读面各自守恒保持、互不串账、reset 独立隔离。零生产改动。

## 兼容性

纯测试增量；无生产代码变更。

## Out of Scope

- 其余 memory 读面组合枚举（按需另轮）。
