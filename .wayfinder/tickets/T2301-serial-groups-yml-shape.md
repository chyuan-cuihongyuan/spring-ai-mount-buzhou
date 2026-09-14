---
id: T2301
title: serial-groups yml 通道（F2 残留收口）的形状裁决
type: task
status: closed
assignee: zcode-m
blocked-by:
created: 2026-09-15
---

## Question

M 会话第 28 轮：design-incompleteness F2 残留（buzhou.tool-policies.<name>.serial-group 键全仓零读取）如何收口？

## Resolution

**用户常设授权 AFK（可推翻）**

形状：ToolsModule.fromYml 加 `serial-groups` map 键（工具名→组名）——configure() 合并时 yml 显式覆盖 @BuzhouTool 注解通道（同名 yml 优先）；无注解第三方工具（经 builder 装配的内置工具名空间）亦可纯 yml 指定。F2 超时键已闭环（ToolTimeoutOverrides），本轮 serial-group 键闭环 = F2 全档收口。
