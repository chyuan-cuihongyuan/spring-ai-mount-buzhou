# 1525 — serial-groups yml 通道（F2 残留收口）

> 来源：M 会话第 28 轮 = effort #1525（impl 1128）。design-incompleteness F2 全档闭环（超时键已由 ToolTimeoutOverrides 闭环）。

## 背景

工具级策略键中 `serial-group` 此前仅 @BuzhouTool 注解通道（RuntimeConfig.serialGroups 零 yml 读取）——F2 评审残留。

## 目标

ToolsModule.fromYml 解析 `serial-groups` map（名→组）；configure() 合并 yml 优先覆盖注解。

## 兼容性

未配置 yml 键零变化（注解通道原样）。
