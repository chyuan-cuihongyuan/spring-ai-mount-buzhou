# 1094 — guard 三 hook 链顺序协作组合测试轮

> 来源：J 会话第 94 轮 = effort #1094（[T1643](../../.wayfinder/tickets/T1643-trihook-shape.md) / [T1644](../../.wayfinder/tickets/T1644-trihook-verify.md) / impl 846）。纯测试轮第九弹（R81/R82/R84/R86/R87/R88 先例）。

## Problem Statement

SecretScanHook（40 脱敏）→ SpotlightHook（80 包裹）→ ContentModerationHook（210 检查）三 hook 链顺序协作——三读面在链中的计数一致性无验证。

## 目标

新增 `TriHookChainReadoutTest`（buzhou-guard）：同 ctx 三 hook 顺序调用后 SpotlightStats/ModerationStats 各自守恒保持 + 链路语义计数一致。零生产改动。

## 兼容性

纯测试增量；无生产代码变更。

## Out of Scope

- SecretScanStats 引擎层形状（scanner 层另口径在册）。
