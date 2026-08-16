---
Type: task
Status: closed
blocked-by: T193-eval-runner.md
---
## Question

eval.run.completed 事件：dataset/runId/items/passed/failed/passRate payload，
合成会话事件 dispatch 外发（webhook 监听者零改造口径延续）；失败不外发。

## Resolution

impl-161 落地：AgentSession 新公共面 emitEvent（default UOE + DefaultAgentSession 委托
dispatchEvent）；runner 收尾会话发 eval.run.completed（payload 含 runId/total/passed/failed/
errored/passRate/durationMs）；空集 run 不发（事件语义裁定）；run 失败 fail-fast 无记录无事件。
实现裁定：独立收尾会话替代「末项会话上发」（项会话逐项 close 语义优先，入档）。2 测试绿。
T195 关闭。
