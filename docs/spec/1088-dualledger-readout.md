# 1088 — memory 双台账组合测试轮

> 来源：J 会话第 88 轮 = effort #1088（[T1631](../../.wayfinder/tickets/T1631-dualledger-shape.md) / [T1632](../../.wayfinder/tickets/T1632-dualledger-verify.md) / impl 840）。纯测试轮第六弹（R81/R82/R84/R86/R87 先例）。

## Problem Statement

R63 BiTemporalFactLedger（事实台账）与 R55 EpisodeLedger（情景台账）双台账同域共存——**双台账读面独立性与 reset 隔离**无验证。

## 目标

新增 `DualLedgerReadoutTest`（buzhou-memory）：fact 写入与 episodic record/recall 交叉后，FactLedgerStats 与 EpisodicMemoryStats 各自计数互不串账、reset 独立隔离。零生产改动。

## 兼容性

纯测试增量；无生产代码变更。

## Out of Scope

- 三台账以上组合枚举（按需另轮）。
