---
id: T945
title: hook 计时聚合读面的验证
type: task
status: closed
assignee: zcode-f
blocked-by: T944
created: 2026-09-13
---

## Question

跨链聚合真合并？未装配零变化？健康面 details 与聚合一致？

## Resolution

**用户常设授权 AFK（可推翻）**

验证（F 会话第 48 轮）：① 两条 HookChain（aggregator 开）同名 hook 计数合并（count=两侧之和）；② aggregator 关（缺省）跨链零共享（既有 HookChainTimingTest 零回归——链内口径不变）；③ HookTimingHealth details 含全部 hook 计时（装配测试）；④ 全模块测试绿。
