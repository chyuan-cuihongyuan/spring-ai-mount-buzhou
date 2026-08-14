---
Type: task
Status: open
blocked-by: T83
---
## Question

per-session 配额限流怎么做？现状：ModelRateLimiter 是全局 per-model 单进程桶，单会话可饿死全局；无每日/每会话配额。决策点：配额维度（turns/tool-calls/tokens per session per day？）、计数存放（内存窗口+事件可见，分布式显式不做）、超配额行为（Block+结构化事件 vs 异常）、与 T83 计量的关系（复用累计计数器）。产出 spec 16 增量 + impl 59。
