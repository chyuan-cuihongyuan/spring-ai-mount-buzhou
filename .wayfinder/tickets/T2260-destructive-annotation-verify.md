---
id: T2260
title: BuzhouTool destructive 风险注解的验证裁决
type: task
status: closed
assignee: zcode-m
blocked-by: T2259
created: 2026-09-15
---

## Question

M 会话第 5 轮：注解驱动名单如何验收？

## Resolution

**用户常设授权 AFK（可推翻）**

验证裁决：`mvn -pl buzhou-tools -am test` 绿——
① 既有 enabledDangerousToolNames 断言零变化（行为等价迁移）；
② 新增用例：自定义 @BuzhouTool(destructive=true) 工具经 builder.tools() 装配后自动入名单；
③ 无注解第三方工具不入名单（不误伤）。
