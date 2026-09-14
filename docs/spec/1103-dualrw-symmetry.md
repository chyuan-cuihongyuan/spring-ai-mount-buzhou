# 1103 — 双档读写四象限对照组合测试轮

> 来源：J 会话第 103 轮 = effort #1103（[T1665](../../.wayfinder/tickets/T1665-dualrw-shape.md) / [T1666](../../.wayfinder/tickets/T1666-dualrw-verify.md) / impl 855）。纯测试轮第十四弹。

## Problem Statement

R82 双档对照只覆盖执行档——**读写对跨两档组装的对称恒等**（bytesWritten==bytesRead 在两组装下皆成立）未验证。

## 目标

新增 `DualModeRwSymmetryTest`（buzhou-tools）：同内容两组装 write→read 往返对称恒等 + 双守恒 + reset 隔离。零生产改动。

## 兼容性

纯测试增量；无生产代码变更。

## Out of Scope

- 其他组合枚举（按需另轮）。
