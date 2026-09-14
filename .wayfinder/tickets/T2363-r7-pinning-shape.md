---
id: T2363
title: R7 虚拟线程 pinning 审计与金丝雀热路径修复的形状裁决
type: task
status: closed
assignee: zcode-n
blocked-by: T2362
created: 2026-09-15
---

## Question

N 会话第 7 轮：pinning 风险全修还是审计入档+Top1 修复？

## Resolution

选 **审计入档 + Top1 修复**。17 组风险一次性全修超出单轮纵切片；且多数组是
低频路径（每进程一次的建连/DDL、低频轮换）。Top1（CanaryToolCallback.route）是
唯一每次工具调用的热路径——三段式修复便宜且语义损失最小（计数原子不变，回滚
判定最多晚一次调用）。其余组排队后续独立小轮（写线程化 / store-CAS 降锁 /
建连锁外化三家族）。
