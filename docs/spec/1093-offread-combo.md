# 1093 — offload→readBack 双轴闭环组合测试轮

> 来源：J 会话第 93 轮 = effort #1093（[T1641](../../.wayfinder/tickets/T1641-offread-shape.md) / [T1642](../../.wayfinder/tickets/T1642-offread-verify.md) / impl 845）。纯测试轮第八弹（R81/R82/R84/R86/R87/R88 先例）。

## Problem Statement

R77 SpillOffloadHook（溢出落盘）与 R62 ReadRangeTool（分段回读）构成溢出闭环——**写读双向计数一致性**（offloaded 后 readBack 必然命中）无组合验证：闭环断裂（溢出成功但回读失败）= 数据丢失信号。

## 目标

新增 `OffloadReadBackComboTest`（buzhou-spill）：超阈值输出溢出 → read_range 回读闭环——SpillOffloadStats 与 ReadRangeStats 各自守恒 + offloaded/reads 闭环对应。零生产改动。

## 兼容性

纯测试增量；无生产代码变更。

## Out of Scope

- 墓碑收缩视图面（视图生成侧另轴）。
