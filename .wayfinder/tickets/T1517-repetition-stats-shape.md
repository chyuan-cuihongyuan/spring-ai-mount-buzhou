---
id: T1517
title: 打转检测触发聚合读面的形态裁决
type: task
status: closed
assignee: zcode-j
blocked-by:
created: 2026-09-14
---

## Question

J 会话第 33 轮：打转检测触发聚合读面在本仓是否有缺口？形态如何裁决？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（J 会话第 33 轮 = effort #1032 / spec 1032 / impl 785）：缺口成立——RepetitionDetectorHook（spec 326）只有 per-session currentRun 即时读数，**无跨会话触发聚合**：打转 fire 多少次、unstick 拦了多少、历史最大 run 长多少不可见——检测阈值（window/相似度）调参与打转频率相关性无据。落点 core/runaway：实例级 fires/blocks 两 AtomicLong + maxRunSeen（verdict runLength 峰值 CAS）+ 嵌套 record `RepetitionStats(fires, blocks, maxRunSeen)` + `stats()`。afterModel 行为逐位不变（fire/block/CONTINUE 原语义）；maxRunSeen 只统计 verdict 时刻值（闩后延续不计——诚实入档）。
