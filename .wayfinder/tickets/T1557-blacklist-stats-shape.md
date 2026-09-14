---
id: T1557
title: 命令黑名单拦截判定读面（CommandBlacklistStats）的形状裁决
type: task
status: closed
assignee: zcode-j
blocked-by: T1555
created: 2026-09-15
---

## Question

J 会话第 51 轮：tools/command 域的读面增量选什么形状？

## Resolution

**用户常设授权 AFK（可推翻）**

选题（跨模块批量扫描命中，tools/command 三类全零计数中取最小完整面）：CommandBlacklist.matches() 纯布尔静默拦截——黑名单命中频次与放行比不可见（模型反复试探危险命令 vs 偶发命中无法区分）。Fail2ban 规则命中计数思想。

形状裁决：`CommandBlacklist` 内静态 `AtomicLong` 三计数——checks（matches 入口）/ matched（true=拦截）/ allowed（false=放行，含空白命令短路路径——语义即未拦截，口径诚实）；嵌套 `record CommandBlacklistStats` + `stats()` + `resetForTest()`。守恒 `checks = matched + allowed`。静态面理由同 R46–R49 先例。matches() 返回值逐位不变。

Out of scope：按 pattern 分桶计数（模式清单是配置面，命中分布留后续轮按需）；RunCommandTool 执行结果分布（另轮分轴）。
