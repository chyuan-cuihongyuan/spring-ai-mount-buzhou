---
Type: task
Status: open
blocked-by:
---
## Question

会话 fork/branch 怎么做？借鉴：LangGraph time-travel fork（checkpoint 历史取任意点分支重放）。现状：快照存储已在（snapshots 表、RunStateSnapshot），resume 有，fork 无。决策点：fork 源（从最后消息 vs 指定 messageId 截断）、store 层复制语义（Message/Summary/SessionState 三 store 的 copy-to-new-session）、evidence-id/spill 产物归属（共享只读）、新 sessionId 语义与事件、与 memory 压缩历史的关系。产出 spec 20 + impl 63。
