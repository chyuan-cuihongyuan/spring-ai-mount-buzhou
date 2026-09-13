# effort #735 — 事件配对完整性审计

- 会话：G 会话 700 系第 36 轮 ｜ spec [735](../../../docs/spec/735-event-pairing-audit.md) ｜ 票 [T1070](../tickets/T1070-event-pairing.md)/[T1071](../tickets/T1071-event-pairing-verify.md) ｜ impl635
- 借鉴：观测对偶配对（DANGLING_REPAIR 的上游信号面）

## 勘察（排重）

- TOOL_INPUT/TOOL_OUTPUT、HITL_REQUEST/HITL_DECISION 自然配对；DANGLING_REPAIR 是修复动作非检测；grep -i pairing 零命中。

## 决定

EventPairingAudit.audit(events, requestToResponse) 纯函数：spanId 内 min 配对，差集产出 UNPAIRED_REQUEST/UNPAIRED_RESPONSE；规则调用方供给泛化任意配对族。

## 测试

配对/悬空/孤儿区分 + HITL 规则 + 空表 null。
