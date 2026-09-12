# 735 — 事件配对完整性审计

> 来源：G 会话第 36 轮 = effort #735（观测对偶配对）/ [T1070](../../.wayfinder/tickets/T1070-event-pairing.md) / [T1071](../../.wayfinder/tickets/T1071-event-pairing-verify.md) / impl 635。

## Problem

TOOL_INPUT 无 TOOL_OUTPUT（工具调用被中断/崩溃）、HITL_REQUEST 无 HITL_DECISION（人工审批悬空）——「应答缺失」散在事件流里没有审计面。DANGLING_REPAIR 是修复动作记录，不是检测。

## Solution

EventPairingAudit.audit(events, requestToResponse) 纯函数：规则表调用方供给；spanId 内 min(请求,应答) 配对——差集产出 UNPAIRED_REQUEST / UNPAIRED_RESPONSE（spanId+eventId 定位）；paired 计数。

## Out of Scope

自动重放/修复（DANGLING_REPAIR 既有）；跨 span 配对。
