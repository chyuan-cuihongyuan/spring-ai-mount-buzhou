# 1091 — 冒烟清单扩展轮

> 来源：J 会话第 91 轮 = effort #1091（[T1637](../../.wayfinder/tickets/T1637-readout-ext-shape.md) / [T1638](../../.wayfinder/tickets/T1638-readout-ext-verify.md) / impl 843）。纯测试轮（R83 元验证轮的清单维护轮）。

## Problem Statement

R83 建立的 ReadoutContractSmokeTest 清单漏纳 R78/R79 两读面——登记纪律要求清单随读面产出同步扩展，遗漏即冒烟缺口。

## 目标

清单追加 `ArchivePurgeJob`（R78）与 `SpillCipher`（R79）——17 成员全量冒烟。零生产改动。

## 兼容性

纯测试增量；无生产代码变更。

## Out of Scope

- 非统一形状读面（TodoTool actionStats/ToolSlowLog reset()——R83 已在册）。
