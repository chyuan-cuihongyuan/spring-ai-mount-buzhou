---
id: T2277
title: 中断与异常上下文卫生轮（design-incompleteness 五-4/五-8 部分）的形状裁决
type: task
status: closed
assignee: zcode-m
blocked-by:
created: 2026-09-15
---

## Question

M 会话第 15 轮：五-4（吞 InterruptedException 不恢复）与五-8（异常 message 缺上下文）如何清扫？

## Resolution

**用户常设授权 AFK（可推翻）**

① mcp DefaultMcpClientRegistry.shutdown 的排空等待 catch(Exception ignored) 吞 InterruptedException 不恢复中断位（JCIP 违规 + R5 审计漏网——一把抓把中断包进去）：收窄为 TimeoutException|ExecutionException（强杀兜底语义不变）+ InterruptedException 独立分支恢复中断位并 break（调用方取消意图优先于排空）；
② DiskSpillStore 9 处 "spill 磁盘 IO 失败" 裸 message 补操作上下文（操作名+sessionDir/rootDir/uri/metaPath/sessionId——store() 的带 uri 先例对齐，五-8 闭环）。

副产出记档：HEAD 快照 CounterAtomicitySpreadTest 既有失败（TokenBudgetHook.afterModel 对 request()=null 的 ModelCallContext NPE——非本线产物，留归属会话排查，R21 预检轮复查）。
