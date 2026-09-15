---
id: T2891
title: argv 预算门的形状裁决
type: task
status: closed
assignee: zcode-o
blocked-by: []
created: 2026-09-16
---

## Question]

命令参数的体量约束怎么前移？（spec 1845 / effort #1845 / R46）

## Resolution

**Linux execve ARG_MAX/MAX_ARG_STRLEN 思想纯校验 `ArgvBudgetGate`
（buzhou-tools）**：totalBytes 字节账（每参字符数+1 计 NUL）+ verify 三态
FIT/OVER_TOTAL/OVER_SINGLE_ARG（单参闸先于总量闸——诊断价值高；边界含上）
+ 默认 1MiB/128KiB 常量；E2BIG 从 exec 层前移到参数校验层。

