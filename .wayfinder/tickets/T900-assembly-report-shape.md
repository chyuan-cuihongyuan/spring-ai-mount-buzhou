---
id: T900
title: 启动装配摘要的形态裁决
type: task
status: closed
assignee: zcode-f
blocked-by:
created: 2026-09-12
---

## Question

Spring Boot diagnostics/actuator startup 的思想：启动后一眼看清「这套进程装了什么」。buzhou 的机制开关散在十余个 @ConditionalOnProperty——运维要翻 yml 才知道生效面。摘要怎么给？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（F 会话第 26 轮 = effort #600 / spec 625 / impl 478）：

1. `BuzhouAssemblyReport`（core，ApplicationReady 监听）：一行 INFO 输出机制开关（十键固定清单）+ store.type + model-name 的生效面板；缺省键显示默认值（safe-by-default 矩阵：otel/dashboard 关、其余开）。
2. opt-in：`buzhou.assembly-report.enabled=true` 声明即装（默认无 bean 零变化）。
3. summary() 静态可测（测试/导出面与日志同源）；另：错误签名窗化主题经核查可由 spec112+121 差分导出推出——ruled-out。
