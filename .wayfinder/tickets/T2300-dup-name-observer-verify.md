---
id: T2300
title: 配置错误显形双小项的验证裁决
type: task
status: closed
assignee: zcode-m
blocked-by: T2299
created: 2026-09-15
---

## Question

M 会话第 27 轮：双小项如何验收？

## Resolution

**用户常设授权 AFK（可推翻）**

验证裁决：HookDuplicateNameWarnTest 两用例 + HookChainTest/GuardBlockObserverClosureTest 零回归——① 同名双 hook 链可构建、composition 保留两名；② 同 observer 双注册单份通知（open/start/end 各恰一次）。
