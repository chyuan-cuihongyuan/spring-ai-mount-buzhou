---
id: T1597
title: http_request 受控头丢弃显形（headerDrops 补桶）的形状裁决
type: task
status: closed
assignee: zcode-j
blocked-by: T1595
created: 2026-09-15
---

## Question

J 会话第 71 轮：tools/http 域的增量选什么形状？

## Resolution

**用户常设授权 AFK（可推翻）**

选题：HttpRequestTool 的连接级/逐跳头黑名单（impl-49：host/content-length/transfer-encoding/connection）命中即**静默 return 跳过**——模型带敏感头试探（host 覆盖/ smuggling 试探）的频次完全不可见。受控头试探是 prompt injection 间接信号（OWASP HTTP header injection 试探率思想）。

形状裁决：`HttpRequestTool` 静态 `AtomicLong` 增第 9 计数 `headerDrops`（黑名单命中丢弃处单点）+ 并入既有 `HttpToolStats` record 尾参（**字段追加非破坏**——record 换 publicly 语义上的位置参数追加在 0.x 语义允许范围，且该 record 未被外部引用）；stats()/resetForTest() 同步。守恒式不变（headerDrops 为旁路修正量不占入口桶——一次请求可丢多头）。其余语义逐位不变。

Out of scope：按头名分桶（黑名单清单即配置面）；请求头白名单模式（另立）。
