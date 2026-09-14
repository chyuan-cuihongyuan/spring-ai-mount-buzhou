---
id: T2319
title: 评估 run 进度读面的形状裁决
type: task
status: closed
assignee: zcode-m
blocked-by: T2261
created: 2026-09-15
---

## Question

M 会话第 38 轮：长评估 run 的进度观测（宿主轮询 UI/日志/探活）如何提供？

## Resolution

**用户常设授权 AFK（可推翻）**

EvalRunner.progress() → EvalRunProgress(runId, done, total, cancelled) 不可变快照——串行每项后/波间/cancelled 占位分支三处更新（volatile 写，读面无锁）；done 含 pruned/cancelled 占位项（终态即完成）；无活跃 run 为最近一次终态快照。进度条思想（tqdm）——run 是同步方法，宿主从另一线程轮询。
