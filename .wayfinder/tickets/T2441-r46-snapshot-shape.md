---
id: T2441
title: R46 快照增量与冲突化解的形状裁决
type: task
status: closed
assignee: zcode-n
blocked-by: T2440
created: 2026-09-15
---

## Question

N 会话第 46 轮：同文件并行双插如何处置？

## Resolution

**保留 M 系撤 N 方**。M 版（spec 1508 注释 + dangerousTools 字段链）语义
完整；我方为解卡而插的读数是 M 系意图的重复实现。冲突暴露共用工作区并发
编辑风险——文本级去重后 guard 全量回归绿即收。
