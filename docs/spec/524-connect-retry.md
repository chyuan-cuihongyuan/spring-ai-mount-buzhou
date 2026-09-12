# Spec 524 — MCP 建连退避重试（effort #524）

> wayfinder map：`.wayfinder/maps/effort-524.md`（T801–T802）。E 会话第 25 轮。

## Problem Statement

registry 建连失败只记 ERROR+跳过——server 暂时不可达（启动顺序竞态/
网络抖动）在下次 refresh 前永久缺席，工具集静默缩水。

## Solution

`ConnectRetryPolicy`（maxAttempts/baseDelayMillis）+ addEntryWithRetry：
失败按 base×2^(attempt-1) 封顶 60s 在 scheduler 线程延迟重排；重试耗尽
收口既有失败语义（ERROR Event+计数，不新增状态）；每次重排计
`buzhou.mcp.connect.retries`。yml `buzhou.mcp.connect-retry.{max-attempts,
base-delay-ms}` 声明即启用（默认不重试——零默认行为变化）。

## User Stories

1. 作为 MCP 宿主，我想 server 暂时不可达时自动退避重试， so 启动顺序
   竞态自愈而非工具集静默缩水。

## Implementation Decisions

- 重试在 scheduler 线程（不占刷新锁——refresh 语义不变）。
- 耗尽后与既有「跳过」语义完全一致（不新增状态机）。

## Testing Decisions

- FlakyFactory：2 败后成功 → 重试至 ACTIVE（connects≥3）；永不成功 →
  耗尽后既有失败语义；策略校验 fail-fast；yml 解析。

## Out of Scope

- 无限重试；jitter；建连熔断。

## Further Notes

- 顶层新增 `ConnectRetryPolicy`（registry 嵌套 record）随轮快照核对。
