---
Type: task
Status: closed
blocked-by: T203-cache-advisor-key.md
---
## Question

写入边界：只缓存终态响应（无 toolCalls、有非空内容）；带 toolCalls/异常/空响应不写；
命中重放的 response 只读性断言。

## Resolution

impl-169 落地：isTerminal 公开钉住（无 toolCalls 且非空）；本地裁定差异（LiteLLM 无 agent
工具副作用约束）入档；单元三分判定绿。T204 关闭。
