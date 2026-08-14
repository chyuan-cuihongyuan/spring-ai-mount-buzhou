---
Type: task
Status: open
blocked-by:
---
## Question

事件外发 webhook 怎么做？现状：SessionEventListener 仅进程内，唯一外发是 OTLP span；webhook 零支持。借鉴：OpenHands event stream、Dify webhook、GitHub webhook（HMAC 签名+重试+幂等）。决策点：挂点（core EventDispatch 旁路 sink vs 独立 listener 模块）、投递语义（at-least-once+幂等键，有界队列满则丢弃+计数）、签名（HMAC-SHA256 header）、失败重试（有限次+退避）、配置与开关默认关、事件 payload 形态（SessionEvent JSON）。产出 spec 20 增量 + impl 64。
