---
id: T1641
title: offload→readBack 双轴闭环组合测试轮的形状裁决
type: task
status: closed
assignee: zcode-j
blocked-by: T1629
created: 2026-09-15
---

## Question

J 会话第 93 轮：offload 与回读双轴闭环的增量选什么形状？

## Resolution

**用户常设授权 AFK（可推翻）**

选题：R77 SpillOffloadHook（溢出落盘）与 R62 ReadRangeTool（分段回读）构成溢出闭环——**写读双向计数一致性**（offloaded 后 readBack 必然成功命中）无组合验证。纯测试轮第八弹。

形状裁决：新增 `OffloadReadBackComboTest`（buzhou-spill）——超阈值输出溢出（offloaded）→ read_range 回读（reads=1 完整命中）闭环计数一致性 + 双读面各自守恒。零生产改动。
