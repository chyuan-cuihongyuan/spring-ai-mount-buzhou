---
id: T2163
title: 会话关闭耗时读数（SessionCloseStats）的形状裁决
type: task
status: closed
assignee: zcode-l
blocked-by: 
created: 2026-09-14
---

## Question

L 会话第 31 轮：会话关闭排空健康面的形状选什么？

## Resolution

**用户常设授权 AFK（可推翻）**

勘察：close() 清理优先异常聚合全程无计时——排空慢不可见。

形状裁决：SessionCloseStats 进程级静态面（公共类）——closed/closeFailures/lastCloseDurationMillis/maxCloseDurationMillis 水位+Snapshot/resetForTest；埋点 close() 计时+failures 非空记失败（清理优先/聚合/幂等语义逐位不变）。

Out of scope：observer 分项计时；排空中断；spawn 计时（823 已有）。
