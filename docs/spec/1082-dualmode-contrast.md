# 1082 — 双档 run_command 对账组合测试轮

> 来源：J 会话第 82 轮 = effort #1082（[T1619](../../.wayfinder/tickets/T1619-dualmode-shape.md) / [T1620](../../.wayfinder/tickets/T1620-dualmode-verify.md) / impl 834）。纯测试轮（R81 先例第二弹）。

## Problem Statement

R74 入册的「timeout=0 沙箱档补默认值送达 vs 直执行档显式拒绝入桶」语义分叉无测试钉住：未来重构可能被当 bug「修齐」或无意识回归。

## 目标

新增 `DualModeRunContrastTest`（buzhou-tools）：同命令序列双档对照——timeout=0 直执行档入 timeoutParamRejects 桶、沙箱档送达入 runs 桶；两档各自守恒恒等式保持。零生产改动。

## 兼容性

纯测试增量；无生产代码变更。

## Out of Scope

- 其他两档差异点枚举（按需另轮）。
