# 1114 — evict×readRange 逐出复活组合测试轮

> 来源：J 会话第 117 轮 = effort #1117（[T1693](../../.wayfinder/tickets/T1693-evictread-shape.md) / [T1694](../../.wayfinder/tickets/T1694-evictread-verify.md) / impl 865）。纯测试轮第十八弹。

## Problem Statement

R53 逐出与 R62 回读构成"逐出→回读复活→再逐出"生命周期闭环——闭环中双读面（EvictStats/ReadRangeStats）计数一致性无组合验证。

## 目标

新增 `EvictReadBackComboTest`（buzhou-spill）：逐出→回读→再逐出链——双 stats 各自计数一致 + 守恒。零生产改动。

## 兼容性

纯测试增量；无生产代码变更。

## Out of Scope

- TTL 自动逐出面（R88 已覆盖）。
