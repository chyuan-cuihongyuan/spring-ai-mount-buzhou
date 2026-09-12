# 625 — 启动装配摘要

> 借鉴：[spring-projects/spring-boot](https://github.com/spring-projects/spring-boot) diagnostics report / actuator startup 面板。
> 来源：F 会话第 26 轮 = effort #600 / [T900](../../.wayfinder/tickets/T900-assembly-report-shape.md) / [T901](../../.wayfinder/tickets/T901-assembly-report-verify.md) / impl 478。

## 背景

机制开关散在十余个 @ConditionalOnProperty——「这套进程装了什么」要翻 yml+代码对账；运维/工单第一问没有一屏答案。

## 目标

`BuzhouAssemblyReport`：ApplicationReady 后一行 INFO 生效面板（机制开关 + store + model）。

## 非目标

- 不做运行时面板端点（343 生效配置端点已覆盖——本报告是启动时点的日志摘要）。

## 设计

十键固定清单 + store.type + model-name；缺省键显示默认值；`buzhou.assembly-report.enabled=true` opt-in；summary() 静态与日志同源。

## 测试

3 用例：缺省矩阵 / 覆盖值 / opt-in 门。

## 兼容性

默认无 bean 零变化；新增公共类型随轮再生快照。
