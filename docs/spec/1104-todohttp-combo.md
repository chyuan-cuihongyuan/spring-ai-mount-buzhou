# 1104 — todo×http 跨工具工作流组合测试轮

> 来源：J 会话第 104 轮 = effort #1104（[T1667](../../.wayfinder/tickets/T1667-todohttp-shape.md) / [T1668](../../.wayfinder/tickets/T1668-todohttp-verify.md) / impl 856）。纯测试轮第十五弹。

## Problem Statement

R40 TodoTool 与 R49 HttpRequestTool 在同会话工作流协同（HTTP 取数→todo 更新）——**双读面独立性**无验证：串账会破坏两工具各自的对账能力。

## 目标

新增 `TodoHttpComboTest`（buzhou-tools）：同会话 http 调用与 todo upsert 交叉后，HttpToolStats 与 TodoActionStats 各自计数独立（互不串账）+ reset 独立。零生产改动。

## 兼容性

纯测试增量；无生产代码变更。

## Out of Scope

- 跨工具业务语义联动（todo 内容与 HTTP 响应的关联归模型）。
