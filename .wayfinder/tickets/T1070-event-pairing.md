---
id: T1070
title: 事件配对完整性审计的裁决
type: task
status: closed
assignee: zcode-g
blocked-by:
created: 2026-09-13
---

## Question

悬空工具调用/HITL 悬空审批无检测面——加配对审计吗？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（G 会话第 36 轮 = effort #735 / spec 735 / impl 635）：EventPairingAudit.audit(events, requestToResponse) 纯函数——规则表调用方供给，spanId 内 min 配对，差集产出 UNPAIRED_REQUEST/UNPAIRED_RESPONSE+paired 计数。泛化任意配对族。
