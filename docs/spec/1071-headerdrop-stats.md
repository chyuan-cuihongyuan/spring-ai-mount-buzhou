# 1071 — http_request 受控头丢弃显形

> 来源：J 会话第 71 轮 = effort #1071（[T1597](../../.wayfinder/tickets/T1597-headerdrop-stats-shape.md) / [T1598](../../.wayfinder/tickets/T1598-headerdrop-stats-verify.md) / impl 823）。借鉴：OWASP header injection 试探率（受控头试探频次是间接攻击信号）。R49 http 请求读面的旁路补桶微轮。

## Problem Statement

`HttpRequestTool` 的连接级/逐跳头黑名单（impl-49：host/content-length/transfer-encoding/connection——防 HTTP 语义破坏与 smuggling 向量）命中即静默 return 跳过：**受控头试探频次不可见**。模型反复携带 Host/Transfer-Encoding 类头是 header injection 试探的直接信号，丢弃行为无量化即无对账。

## 目标

- `HttpRequestTool` 增量：`HttpToolStats` 追加第 9 计数 `headerDrops`（黑名单命中丢弃处单点计数）。
- 守恒式不变：headerDrops 为**旁路修正量**（一次请求可丢多头，不占入口桶）。
- `HttpToolStats` record 尾参追加（0.x 语义允许；该 record 无外部引用）。

## 兼容性

纯增量读面：请求构造、黑名单丢弃行为、响应语义逐位不变；无新配置项。

## Out of Scope

- 按头名分桶（黑名单清单即配置面）。
- 请求头白名单模式（另立）。
