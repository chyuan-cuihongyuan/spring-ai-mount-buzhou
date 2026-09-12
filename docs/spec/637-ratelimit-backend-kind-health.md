# 637 — 限流后端形态进健康面

> 来源：F 会话第 38 轮 = effort #600（spec 614 装配后的生效确认面）/ [T924](../../.wayfinder/tickets/T924-rl-backend-kind-shape.md) / [T925](../../.wayfinder/tickets/T925-rl-backend-kind-verify.md) / impl 490。

## 背景

GCRA yml 声明后是否生效（含被共享后端覆盖）无健康面字段。

## 目标

`stats.details().rateLimitBackend`（memory / memory-gcra / redis / none）。

## 设计

configure 时经 limiter.backend().kind() 一次性写入；volatile 只写一次。

## 测试

1 用例三态 + 全模块零回归。

## 兼容性

details 加键纯增量。
