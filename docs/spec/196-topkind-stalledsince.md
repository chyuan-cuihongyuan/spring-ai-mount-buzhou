# Spec 196 — 签名分面 top + 心跳单查（effort #142）

> wayfinder map：`.wayfinder/maps/effort-142.md`（T559–T560）。双小方法轮。

## Solution

①`ErrorSignatures.top(kind, n)`：按 kind 前缀过滤的分面排行（「只看模型侧
错误族」类看板；空白 kind fail-fast）。②`TurnHeartbeat.stalledSince(sessionId,
threshold, now)`：单会话停滞时长查询（超阈返回 Duration，未超/未注册 null——
单点排障面，与批量 stalled 互补）。

## Testing Decisions

- 红队：前缀过滤 + 空 kind 拒绝；超阈/未超/未注册三态。
