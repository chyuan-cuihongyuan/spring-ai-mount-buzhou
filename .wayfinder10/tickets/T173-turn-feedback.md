---
Type: task
Status: open
---
## Question

AgentSession.rateTurn(turnSeq, type=boolean|numeric|categorical, value, comment, source=user|implicit)——落 EventRecord（turn.feedback 事件）+ webhook 外发；校验 turnSeq 存在、value 域合法；Langfuse score API 语义（挂 turn 级）。验证：API 单测 + 事件断言。
