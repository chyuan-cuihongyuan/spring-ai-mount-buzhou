---
id: T1321
title: 工具调用结局分布读面的形态裁决
type: task
status: closed
assignee: zcode-i
blocked-by:
created: 2026-09-14
---

## Question

I 会话第 44 轮：ToolCallLog（事件溯源工具日志，spec 50）逐条记录 outcome——「COMPLETED/FAILED/TIMEOUT/CANCELLED 四桶分布」聚合读面是否有缺口？形态如何裁决？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（I 会话第 44 轮 = effort #944 / spec 944 / impl 693）：缺口成立——exactly-once 证据层的结局分布不可读（TIMEOUT 占比高 = 工具超时配置问题；CANCELLED = 用户中断/取消风暴）。落点 recovery 包新公共纯函数 `ToolCallOutcomeStats`：`stats(List<ToolCallLogEntry>)` 四桶计数 + `total()` 守恒便捷；record 嵌套桶。纯函数零 IO（RecoverySupport 读出 entries 喂入——与 908/936 纯函数纪律同款）。
