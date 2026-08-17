# 161 — 评估事件外发

**Parent:** spec 52 §F / [T195](../tickets/T195-eval-events.md)

**Status:** done

- [x] AgentSession 新公共面 `emitEvent(type, payload)`（default UOE；DefaultAgentSession 委托
  dispatchEvent；与 rateTurn 同模式）
- [x] EvalRunner 收尾会话 `eval-<runId>-done` 发 `eval.run.completed`
  （实现裁定：独立收尾会话替代 spec 原案「末项会话上发」——项会话逐项 close 资源语义优先，入档）
- [x] 空集 run 不发（事件语义 = 评估完成，非 run 建档）；payload 含 runId/汇总/passRate/durationMs
- [x] 2 测试绿（全局 listener 收到事件 + 空集零事件 + 宿主自定义事件直通）
