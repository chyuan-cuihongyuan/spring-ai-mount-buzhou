---
id: T1173
title: 半开探测成功率读数的形态裁决
type: task
status: closed
assignee: zcode-h
blocked-by: []
created: 2026-09-13
---

## Question

探测质量读数与 811 如何分工？streak 语义如何定？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（H 会话第 37 轮 = effort #836 / spec 836 / impl 589）：`HalfOpenProbeStats`——成败累计+streak 成功清零+近窗 20 环成功率；模型封顶 32；与 811 跳闸频次互补（探测质量）；喂点手动不控许可。
