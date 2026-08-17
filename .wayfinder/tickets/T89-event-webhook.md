---
Type: task
Status: closed
assignee: zcode
blocked-by:
---
## Question

事件外发 webhook 怎么做？现状：SessionEventListener 仅进程内，唯一外发是 OTLP span；webhook 零支持。借鉴：OpenHands event stream、Dify webhook、GitHub webhook（HMAC 签名+重试+幂等）。决策点：挂点（core EventDispatch 旁路 sink vs 独立 listener 模块）、投递语义（at-least-once+幂等键，有界队列满则丢弃+计数）、签名（HMAC-SHA256 header）、失败重试（有限次+退避）、配置与开关默认关、事件 payload 形态（SessionEvent JSON）。产出 spec 20 增量 + impl 64。

## Resolution

AFK 自决（授权同 T81，可推翻）：

1. **挂点**：core `webhook/WebhookEventForwarder implements SessionEventListener`（配置了 url 才装配，默认关）；core 增**全局监听挂点**——`DefaultAgentRuntime.addGlobalEventListener`（doSpawn 后逐会话挂 + 已活跃会话补挂），core auto-config 收集 `SessionEventListener` bean 注入。不新模块、不改 EventDispatch 管线（旁路不增加主链开销）。
2. **投递语义**：**at-least-once**（幂等键 `eventId` = UUID，消费方按需去重——不承诺 exactly-once，fog 已记）；单虚拟线程分发器 + 有界队列（默认 256），**满则丢弃 + 计数**（不阻塞会话事件主链）；优雅关闭：停分发线程前排空剩余队列（限时）。
3. **签名**：配置 secret 时每个请求带 `X-Buzhou-Signature: hex(HMAC-SHA256(secret, body))` + `X-Buzhou-Event-Id`；无 secret 不带签名头。
4. **重试**：IOException/5xx 退避重试（1s×2^n，默认 max 3 次）；4xx 不重试（配置错误类）；全败丢弃 + 计数（webhook.delivery.failures）。
5. **HTTP**：JDK `java.net.http.HttpClient`（零新依赖），POST application/json，timeout 默认 5s。
6. **payload**：`{eventId, sessionId, type, payload, occurredAt}` JSON（Jackson，core 已有）。
7. **观测**：指标 `buzhou.webhook.delivered / dropped / failures`；日志 ERROR 仅全败。
