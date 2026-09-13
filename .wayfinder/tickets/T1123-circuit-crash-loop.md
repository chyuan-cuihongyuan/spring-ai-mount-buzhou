---
id: T1123
title: 断路器 crash-loop 检测的形态裁决
type: task
status: closed
assignee: zcode-h
blocked-by: []
created: 2026-09-13
---

## Question

「恢复即再炸」如何状态化？闩锁语义与解除路径如何定？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（H 会话第 12 轮 = effort #811 / spec 811 / impl 564）：`CircuitCrashLoopDetector`——滑窗 ≥minOpens(≥2) 转闩锁；闩锁唯 recordRecovery 解（窗口滑过不解——k8s 对齐）；loopsDetected 进入即计；封顶 32；旁路喂点不改断路器。
