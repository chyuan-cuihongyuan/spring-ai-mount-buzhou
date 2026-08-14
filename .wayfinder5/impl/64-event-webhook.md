# 64 — 事件外发 webhook（T89 决策落地）

**What to build:** core `webhook/WebhookEventForwarder`（有界队列+虚拟线程分发+HMAC 签名+退避重试）+ `BuzhouWebhookProperties` + DefaultAgentRuntime 全局监听挂点 + auto-config 接线。

**Blocked by:** None.

**Status:** done

- [ ] BuzhouWebhookProperties（url/secret/timeout/max-attempts/queue-capacity；无 url=不装配）
- [ ] WebhookEventForwarder：onEvent 入队（满丢弃+计数）；分发线程 POST JSON + 签名头 + 幂等键头 + 重试；close 限时排空
- [ ] DefaultAgentRuntime.addGlobalEventListener + doSpawn 挂载 + 活跃会话补挂
- [ ] core auto-config：SessionEventListener bean 收集 + webhook 条件装配
- [ ] 测试：JDK HttpServer 收件断言（payload JSON/签名/幂等键）；队列满丢弃；4xx 不重试；5xx 重试后成功

## Done

验证：`./mvnw -pl buzhou-core clean test` 264/264 绿（新增 WebhookEventForwarderTest 3 用例：签名 JSON envelope/5xx 重试后成功/4xx 不重试即弃）。
落地：`webhook/BuzhouWebhookProperties`（无 url=不装配，url 格式 fail-fast）+ `WebhookEventForwarder`（at-least-once、有界队列满丢弃+计数、HMAC-SHA256 签名头、幂等键头、5xx/IO 退避重试、4xx 不重试、close 限时排空、JDK HttpClient 零新依赖、指标三件）；`DefaultAgentRuntime.addGlobalEventListener`（新会话自动挂 + 活跃补挂）；core auto-config 收集 SessionEventListener bean 注入 + webhook 条件装配（destroyMethod=close）。
