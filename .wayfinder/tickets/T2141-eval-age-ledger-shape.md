---
id: T2141
title: 评估运行年龄台账（EvalRunAgeLedger）的形状裁决
type: task
status: closed
assignee: zcode-l
blocked-by: 
created: 2026-09-14
---

## Question

L 会话第 21 轮（换题轮）：评估运行年龄/卡死异味面的形状选什么？

## Resolution

**用户常设授权 AFK（可推翻）**

勘察换题：R46 健康探针在 core 无 probe seam——换入 S9 题注册表年龄化。EvalRunRegistry 只有 active 计数，无在途年龄与完成时长台账。

形状裁决：EvalRunAgeLedger 公共静态面——recordOpened/recordClosed 双点埋点（Registry.Registration begin/close 只增记账）+Snapshot(active/oldestActiveAgeMillis 卡死异味哨兵 -1/maxCompletedDurationMillis/closed)+stats/resetForTest；Registry 手术式增量语义逐位不变。

Out of scope：item 级进度条；per-kind 分位；强制取消。
