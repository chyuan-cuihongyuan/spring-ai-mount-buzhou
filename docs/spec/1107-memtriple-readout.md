# 1107 — memory 三读面大组合测试轮

> 来源：J 会话第 107 轮 = effort #1107（[T1673](../../.wayfinder/tickets/T1673-memtriple-shape.md) / [T1674](../../.wayfinder/tickets/T1674-memtriple-verify.md) / impl 859）。纯测试轮第十三弹。memory 域组合系列收口（R87 双工具/R88 双台账/本弹三读面全交叉）。

## Problem Statement

compact_now、EpisodeLedger、BiTemporalFactLedger 三读面同会话全交叉的组合一致性——两两验证后三读面全交叉的互不串账收口缺失。

## 目标

新增 `MemoryTripleReadoutTest`（buzhou-memory）：三读面交叉调用后各自守恒保持 + 互不串账 + reset 独立隔离。零生产改动。

## 兼容性

纯测试增量；无生产代码变更。

## Out of Scope

- 更多读面组合枚举（按需另轮）。
