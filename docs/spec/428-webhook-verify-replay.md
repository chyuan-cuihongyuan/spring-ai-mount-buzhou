# Spec 428 — Webhook 验签与防重放（effort #428）

> wayfinder map：`.wayfinder/maps/effort-428.md`（T747–T748）。D 会话第 29 轮。

## Problem Statement

forwarder 的 `X-Buzhou-Signature: hex(HMAC-SHA256(secret, body))` 无
时间戳——捕获的请求可无限重放（签名恒过）；库内只有签名方没有验签
方，消费端自写 MAC 比对易错（非常量时间比较是常见时序侧信道漏洞）。

## Solution

`webhook.WebhookSignatures`（Stripe signed webhooks 借鉴）：

- **sign(secret, body)**：hex HMAC-SHA256（与 forwarder 同 crypto 路）。
- **verify(secret, body, hexSig)**：常量时间比对
  （`MessageDigest.isEqual`）；任何缺失/畸形输入 false（fail-closed
  不抛——verify 布尔返回，HTTP 响应语义归消费端）。
- **verify(secret, body, hexSig, timestampHeader, tolerance, now)**：
  验签 + 时间戳容差窗——`|now - ts| <= tolerance` 才过（建议 5m，
  Stripe 同款）；重放窗口有界；时间戳非数字 → false。
- **forwarder 加法变更**：secret 配置时加发
  `X-Buzhou-Timestamp: <epochSeconds>`（不进 MAC——存量验签消费端
  零破坏）。

## User Stories

1. 作为 webhook 消费端，我想一个库内验签工具（常量时间）， so 不自写
   MAC 比对引入时序侧信道。
2. 作为安全负责人，我想重放窗口有界（时间戳容差）， so 捕获的旧请求
   在容差窗外被拒。

## Implementation Decisions

- hmacSha256 复用 forwarder 包内静态实现（同包同 crypto 路——签名/
  验签两侧永不漂移）。
- fail-closed：null/空/畸形一律 false。

## Testing Decisions

- verify：正确签 true；错 secret/篡改 body/垃圾 hex/null false。
- 重放：时间戳=now 过、now-4m（tol 5m）过、now-6m 拒、非数字拒。
- E2E：本地 HttpServer 收 forwarder 请求——X-Buzhou-Signature 与
  X-Buzhou-Timestamp 头齐、`WebhookSignatures.verify`（带时间戳容差）
  对真请求 body 全过（签名方↔验签方往返闭环）。

## Out of Scope

- 签名进时间戳（破坏存量消费者）；nonce 去重；非对称签名。

## Further Notes

- 新公共类型 `WebhookSignatures` 随轮 regenerate 快照。
