# 1089 — spill 域 offload+evict 生命周期组合测试轮

> 来源：J 会话第 89 轮 = effort #1089（[T1633](../../.wayfinder/tickets/T1633-spillchain-shape.md) / [T1634](../../.wayfinder/tickets/T1634-spillchain-verify.md) / impl 841）。纯测试轮第七弹（R81/R82/R84/R86/R87/R88 先例）。

## Problem Statement

R77 SpillOffloadHook（溢出落盘）与 R53 EvictHandleTool（句柄逐出）构成完整生命周期（溢出→逐出→墓碑→回读复活）——两读面在生命周期中的计数一致性无验证。

## 目标

新增 `SpillLifecycleReadoutTest`（buzhou-spill）：溢出→逐出链路后 SpillOffloadStats 与 EvictHandleStats 各自守恒保持、互不串账。零生产改动。

## 兼容性

纯测试增量；无生产代码变更。

## Out of Scope

- 墓碑收缩视图面（视图生成侧另轴）。
