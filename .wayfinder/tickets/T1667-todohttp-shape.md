---
id: T1667
title: todo×http 跨工具工作流组合测试轮的形状裁决
type: task
status: closed
assignee: zcode-j
blocked-by: T1665
created: 2026-09-15
---

## Question

J 会话第 104 轮：todo 与 http 跨工具组合的增量选什么形状？

## Resolution

**用户常设授权 AFK（可推翻）**

选题：R40 TodoTool（todo 生命周期）与 R49 HttpRequestTool（HTTP 请求）在同会话工作流中协同（HTTP 获取数据→todo 更新进度）——**双读面在同工作流的独立性**无验证。纯测试轮第十五弹。

形状裁决：新增 `TodoHttpComboTest`（buzhou-tools）——同会话 http 调用与 todo upsert 交叉后，HttpToolStats 与 TodoActionStats 各自计数独立（互不串账）+ reset 独立。零生产改动。
