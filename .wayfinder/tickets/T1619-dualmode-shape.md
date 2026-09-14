---
id: T1619
title: 双档 run_command 对账组合测试轮的形状裁决
type: task
status: closed
assignee: zcode-j
blocked-by: T1617
created: 2026-09-15
---

## Question

J 会话第 82 轮：两档语义差异的对账增量选什么形状？

## Resolution

**用户常设授权 AFK（可推翻）**

选题：R74 入册的「timeout=0 沙箱档补默认 vs 直执行档显式拒绝」两档语义分叉——差异本身需测试钉住（防未来被当 bug「修齐」或回归）。纯测试轮（R81 先例第二弹）。

形状裁决：新增 `DualModeRunContrastTest`（buzhou-tools）——同命令序列双档对照：timeout=0 直执行档入 timeoutParamRejects 桶、沙箱档送达（补默认 runs）；两档各自守恒恒等式保持。零生产改动。
