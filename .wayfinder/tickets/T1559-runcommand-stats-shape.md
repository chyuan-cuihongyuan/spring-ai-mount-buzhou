---
id: T1559
title: run_command 执行结果分布读面（RunCommandStats）的形状裁决
type: task
status: closed
assignee: zcode-j
blocked-by: T1557
created: 2026-09-15
---

## Question

J 会话第 52 轮：tools/command 域执行侧的读面增量选什么形状？

## Resolution

**用户常设授权 AFK（可推翻）**

选题（command 域第二轴收官）：RunCommandTool.call() 全部分支——空命令 / 黑名单拒绝 / 工作目录不存在 / timeoutSeconds 越界 / 取消 / 超时终止 / 异常兜底 / 正常退出——当前只返回字符串，执行成功率与结局分布不可见。借鉴：Kubernetes Job status conditions（按结局分桶 + conditions 单调）。

形状裁决：`RunCommandTool` 内静态 `AtomicLong` 九计数——attempts（入口）/ exits（正常退出，含非零 exit——进程送达即入此桶）/ canceled（用户取消）/ timeouts（超时终止）/ blankRejects / blacklistRejects / workdirRejects / timeoutParamRejects / failures（catch 兜底）；嵌套 `record RunCommandStats`（totalRejects=四参数桶+failures 派生）+ `stats()` + `resetForTest()`。守恒 `attempts = exits + canceled + timeouts + totalRejects()`。静态面理由同族先例；call() 返回语义逐位不变。

Out of scope：exit code 分布直方图（exits 单桶已回答送达率）；SandboxRunCommandTool 独立计量（装饰同族，读面共享底座）。
