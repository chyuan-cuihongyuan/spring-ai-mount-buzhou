---
id: T2445
title: R48 终验启动的形状裁决
type: task
status: closed
assignee: zcode-n
blocked-by: T2444
created: 2026-09-15
---

## Question

N 会话第 48 轮：终验在主工作区还是隔离 worktree？

## Resolution

选 **隔离 worktree**（/tmp/n-final-verify @ HEAD）。主工作区有 I/J/K/M
多会话并行构建与半成品（历史 32 分钟挂死教训 + 本会话三次编译被挡）——
隔离环境验证的是「HEAD 提交序列的可发布性」这一正确命题。
