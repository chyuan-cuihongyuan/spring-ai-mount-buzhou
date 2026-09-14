# 1105 — EpisodeLedger 双实例组合测试轮

> 来源：J 会话第 105 轮 = effort #1105（[T1669](../../.wayfinder/tickets/T1669-dualinst-shape.md) / [T1670](../../.wayfinder/tickets/T1670-dualinst-verify.md) / impl 857）。纯测试轮第十一弹。

## Problem Statement

R55 EpisodeLedger 静态计数是进程级累计——同 stateStore 多实例（重启语义）场景下计数累计正确性与双守恒无组合验证。

## 目标

新增 `EpisodeLedgerDualInstanceTest`（buzhou-memory）：实例 A/B 共享 stateStore 交叉 record/recall——静态计数跨实例累计正确 + 双守恒 + reset 归零。零生产改动。

## 兼容性

纯测试增量；无生产代码变更。

## Out of Scope

- 跨进程计数持久化（静态计数设计即进程级）。
