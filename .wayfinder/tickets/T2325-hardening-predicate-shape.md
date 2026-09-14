---
id: T2325
title: 进程匹配谓词收窄的形状裁决
type: task
status: closed
assignee: zcode-m
blocked-by:
created: 2026-09-15
---

## Question

M 会话第 41 轮（预检就近处置）：RunCommandHardeningTest.interruptKillsProcessTree 稳定红的根因与修法？

## Resolution

**用户常设授权 AFK（可推翻）**

实证破案：谓词裸 "sleep 30" 子串匹配全机进程命令行——多会话共享机器上并行会话的轮询 shell（for 循环含 sleep 30）命中谓词，assertThat(isAlive).isFalse() 对无关存活进程误红（R41 实证 pgrep 输出命中 N 会话快照循环）。非产品缺陷非 flake——测试谓词过宽。修：锚定 marker 唯一路径（测试自构造命令行的确定性子串）+ 垂死窗口轮询（destroyForcibly 后 SIGKILL 传播窗口）。
