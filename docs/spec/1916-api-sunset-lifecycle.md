# Spec 1916 — API 弃用日落生命周期（effort #1916，R117）

> wayfinder map：`.wayfinder/maps/effort-1800.md`（T3019 替补槽，
> impl 1517）。借鉴：Stripe/GitHub（万星级 API 惯例）版本日落语义
> ——API 版本三阶段 ACTIVE → DEPRECATED（警告头，仍可用）→
> SUNSET（拒绝服务）；days remaining 报表让迁移有倒计时。

## Problem Statement

工具/参数弃用只有「删与不删」两态：弃用公告与实际移除之间是
漫长的灰色期——调用方不知道还剩多久、宿主不知道何时能删，
三段生命周期缺独立判定面。

## Solution

`ApiSunsetLifecycle`（core/policy，静态纯函数 + Phase 枚举）：

- `phase(deprecatedAt, sunsetAt, now)`：now < deprecatedAt → ACTIVE；
  < sunsetAt → DEPRECATED（仍可用但发警告）；≥ sunsetAt → SUNSET
  （拒绝）；
- `daysRemaining(sunsetAt, now)`：剩余毫秒（负 = 已日落，钳 0）。

## User Stories

1. 作为工具作者，弃用公告期 30 天后日落——三段分明迁移有窗。
2. 作为宿主运维者，DEPRECATED 期发警告头不拒服务——温和施压。
3. 作为排障者，daysRemaining 直读——还剩多久一目了然。

## Implementation Decisions

- 纯函数零状态；deprecatedAt ≤ sunsetAt fail-fast；now ≥ 0；
  daysRemaining 下限 0。

## Testing Decisions

- 三段各一例；边界含上两例（恰 deprecatedAt 即 DEPRECATED/恰
  sunsetAt 即 SUNSET）；剩余时间两例（正/已过钳 0）；畸形一型
  fail-fast。

## Out of Scope

- 不做弃用配置存储（归 ToolDeprecationProperties 面）；不做调用
  方通知。

## Further Notes

- 与 ToolDeprecation 配置面互补：那是声明存储，这是时刻判定。
