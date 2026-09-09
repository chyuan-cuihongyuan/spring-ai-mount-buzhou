# Wayfinder Map — Buzhou Webhook 验签与防重放（effort #428，D 会话第 29 轮）

> D 会话第 29 轮（机动轮——R27 勘察旁注展开）。勘察：forwarder 签名
> {@code X-Buzhou-Signature: hex(HMAC-SHA256(secret, body))}——**无时间
> 戳**：捕获的请求可无限重放（签名恒过）；且库内只有签名方没有验签方
> （消费端要自写 MAC 比对——写错成非常量时间比较是常见漏洞）。Stripe
> signed webhooks 的「签名+时间戳+容差窗」协议两侧缺位。

## Destination

`webhook.WebhookSignatures`（消费端验签工具，Stripe 借鉴）：

- `sign(secret, body)`——与 forwarder 同 crypto 路产出 hex；
- `verify(secret, body, hexSig)`——**常量时间**比对（MessageDigest.
  isEqual——防时序侧信道）；
- `verify(secret, body, hexSig, timestampHeader, tolerance, now)`——
  验签+时间戳容差窗（默认建议 5m，Stripe 同款）：|now-ts|≤tolerance
  才过——重放窗口有界；时间戳非数字/缺失 → false（fail-closed）。
- forwarder 加法变更：secret 配置时每请求加发 {@code
  X-Buzhou-Timestamp: <epochSeconds>}（**不进 MAC**——存量验签消费端
  零破坏；新消费端用容差窗验签防重放）。

## Notes

- 号段：spec 428 / T747–T748 / impl-401。
- 借鉴源：Stripe signed webhooks（constant-time MAC 比对+timestamp
  tolerance 防重放）。
- 纪律：verify 布尔返回（消费端自选响应码——库不裁决 HTTP 语义）；
  fail-closed（任何缺失/畸形输入 false 不抛）。

## Out of scope

- 签名进时间戳（破坏性——存量消费者兼容优先，诚实注记）；nonce
  去重（EventDeduplicator 域）；非对称签名（HMAC 共享秘密域）。

## Tickets

- [x] [T747 WebhookSignatures 验签工具](../tickets/T747-webhook-signatures.md)
- [T748 forwarder 时间戳头+E2E](../tickets/T748-webhook-timestamp-header.md)
