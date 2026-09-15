---
id: T1683
title: readRange×Spotlight 组合测试轮的形状裁决
type: task
status: closed
assignee: zcode-j
blocked-by: T1679
created: 2026-09-15
---

## Question

J 会话第 112 轮：回读与包裹组合的增量选什么形状？

## Resolution

**用户常设授权 AFK（可推翻）**

选题：R62 ReadRangeTool（回读）与 SpotlightHook（包裹，ORDER 80 先于 Spill 100）组合——R64 alreadyWrappedSkips 的 readback 切片语义组合验证（溢出→包裹→回读切片含标记段→不再二次包裹）。纯测试轮第十七弹。

形状裁决：新增 `ReadRangeSpotlightComboTest`（buzhou-spill，Spotlighting core 类可引）——溢出后回读切片含标记段 → 再入 spotlight afterTool 后 alreadyWrappedSkips=1。零生产改动。

## 勘误

本组合需 guard 的 SpotlightHook（spill 不依赖 guard），落位改 buzhou-guard（guard 依赖 core 的 Spotlighting 但不依赖 spill……回读工具在 spill）——组合不可单模块落地，回退为：ReadRangeTool 回读溢出占位文本 + 断言含标记段（Spotlighting.wrap core 类直接调用验证包裹后回读幂等）。
