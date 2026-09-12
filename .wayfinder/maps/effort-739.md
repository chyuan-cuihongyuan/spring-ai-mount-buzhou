# effort #739 — 事件静默缺失门

- 会话：G 会话 700 系第 40 轮 ｜ spec [739](../../../docs/spec/739-event-presence-gate.md) ｜ 票 [T1078](../tickets/T1078-event-presence-gate.md)/[T1079](../tickets/T1079-event-presence-gate-verify.md) ｜ impl639
- 借鉴：—（726 分布的对偶面）

## 勘察（排重）

- 726 回答「什么在发生」——「什么该发生而没发生」（生命周期契约）无面；grep -i presenceGate 零命中。

## 决定

`EventTypePresenceGate.gate(events, expectedTypes)` 纯函数——missing 字典序+expectedCount/observedTypes；空契约不误报；null fail-fast。

## 测试
缺失清单/全满足/空契约/null。
