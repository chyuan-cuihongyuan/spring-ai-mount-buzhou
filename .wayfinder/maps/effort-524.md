# Wayfinder Map — Buzhou MCP 建连退避重试（effort #524，E 会话第 25 轮）

> E 会话第 25 轮。勘察：registry 建连失败只记 ERROR Event+计数并跳过
> （不影响其余条目）——server 暂时不可达（启动顺序/网络抖动）在下次
> refresh 前永久缺席。Resilience4j retry 指数退避思想。

## Destination

DefaultMcpClientRegistry `ConnectRetryPolicy`（maxAttempts/baseDelayMillis，
record 校验 fail-fast）+ addEntryWithRetry（delay=base×2^(n-1) 封顶 60s，
scheduler 线程延迟重排；重试耗尽收口既有失败语义——不新增状态）；
计数 buzhou.mcp.connect.retries（server tag）。McpModule.Builder
connectRetry(policy)+fromYml `connect-retry.{max-attempts, base-delay-ms}`
（声明即启用——默认不重试）。

## Notes

- 号段：spec 524 / T801–T802 / impl-427。

## Out of scope

- 无限重试；抖动（jitter）；建连熔断（504 聚合熔断正交）。

## Tickets

- [x] [T801 退避重排](../tickets/T801-connect-retry-backoff.md)
- [x] [T802 yml 装配](../tickets/T802-connect-retry-assembly.md)
