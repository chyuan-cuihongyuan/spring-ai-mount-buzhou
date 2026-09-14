---
id: T2179
title: 悬空轮检测器（DanglingTurnDetector）的形状裁决
type: task
status: closed
assignee: zcode-l
blocked-by: 
created: 2026-09-14
---

## Question

L 会话第 39 轮：取消/中断残留悬空轮的检测面选什么形状？

## Resolution

**用户常设授权 AFK（可推翻）**

勘察：TurnSequenceAudit 管 turn 序号缺号、ConversationShapeAudit 管相邻对——按 turnSeq 分组的「问了没答」形态无归属。

形状裁决：DanglingTurnDetector 纯函数（core/message）——analyze 按 turnSeq 分组，轮内有 USER 无 ASSISTANT 即悬空（TOOL 链不豁免；仅 TOOL/SYSTEM 轮不算）+Report(totalTurns/danglingTurnCount/danglingSamples 升序封顶 8)+hasDangling 哨兵；空输入哨兵；单会话口径。

Out of scope：自动续写；span 层；跨会话聚合。
