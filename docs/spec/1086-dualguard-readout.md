# 1086 — 双守卫（黑名单+SSRF）组合测试轮

> 来源：J 会话第 86 轮 = effort #1086（[T1627](../../.wayfinder/tickets/T1627-dualguard-shape.md) / [T1628](../../.wayfinder/tickets/T1628-dualguard-verify.md) / impl 838）。纯测试轮第四弹（R81/R82/R84 先例）。

## Problem Statement

R51 CommandBlacklist 与 R48 SsrfGuard 两守卫读面在同会话协同——**独立性与一致性**无验证：串账或互相污染会破坏两守卫的独立对账能力。

## 目标

新增 `DualGuardReadoutTest`（buzhou-tools）：混合调用（黑名单命中 + SSRF 拒绝 + 各自放行）后双读面各自守恒保持 + 互不串账 + reset 独立隔离。零生产改动。

## 兼容性

纯测试增量；无生产代码变更。

## Out of Scope

- 其余守卫组合枚举（按需另轮）。
