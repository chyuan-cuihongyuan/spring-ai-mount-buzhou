---
id: T2440
title: R45 @since 补全的验证裁决
type: task
status: closed
assignee: zcode-n
blocked-by: T2439
created: 2026-09-15
---

## Question

N 会话第 45 轮：如何验收？

## Resolution

15/15 文件 @since 覆盖（脚本断言）+ 五模块 test-compile 绿（spill 的
BuzhouSpillAutoConfigurationTest 编译错为并行会话 .m2 旧 core jar 域
——BuzhouLifecyclePhases 在 core main 已存在，非本线产物）。
