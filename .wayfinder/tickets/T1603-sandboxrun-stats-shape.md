---
id: T1603
title: 沙箱版 run_command 执行分布读面（SandboxRunStats）的形状裁决
type: task
status: closed
assignee: zcode-j
blocked-by: T1597
created: 2026-09-15
---

## Question

J 会话第 74 轮：tools/command 沙箱档的读面增量选什么形状？

## Resolution

**用户常设授权 AFK（可推翻）**

选题：SandboxRunCommandTool（128 行独立实现——R52 曾误判为装饰同族而回避，实为独立 call 分支结构）六路径零计数：空命令/黑名单/workdir 拒绝（含非法路径段）/timeout 越界/异常兜底/正常送达。R52 Out of Scope 误判就地撤销。

形状裁决：`SandboxRunCommandTool` 内静态 `AtomicLong` 七计数——calls（入口）/ runs（dispatch 正常返回=送达）/ blankRejects / blacklistRejects / workdirRejects（不存在与非法段合并桶——同为工作目录语义拒绝）/ timeoutParamRejects / failures（catch 兜底）；嵌套 `record SandboxRunStats` + `stats()` + `resetForTest()`。守恒 `calls = runs + 五拒绝桶之和`。静态面理由同族先例；call() 返回语义逐位不变。

Out of scope：launcher 内部取消/超时明细（dispatch 抽象层语义）；按命令分桶（敏感面红线）。
