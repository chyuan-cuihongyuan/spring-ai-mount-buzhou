# 1102 — 冒烟清单第三扩展轮

> 来源：J 会话第 102 轮 = effort #1102（[T1663](../../.wayfinder/tickets/T1663-smokeext3-shape.md) / [T1664](../../.wayfinder/tickets/T1664-smokeext3-verify.md) / impl 854）。R83 元验证轮清单维护第二弹。

## Problem Statement

R85 SkillAdminStats 与 R101 SpillServiceStats 两新读面未纳入统一冒烟清单——登记纪律缺口。

## 目标

清单追加 `SkillAdminApi`（R85）与 `SpillService`（R101）——15→17 成员全量冒烟。零生产改动。

## 兼容性

纯测试增量；无生产代码变更。

## Out of Scope

- 非统一形状读面（R83 在册维持）。
