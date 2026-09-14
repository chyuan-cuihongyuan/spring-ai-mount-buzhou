# 1641 · 负缓存 yml 装配（spec 1633 配置面补全）

> 来源：N 会话 R42（effort #1641 / T2433–T2434 / impl 1194）。

## Solution

`buzhou.core.negative-cache.{enabled,ttl}`：enabled=true 声明即启用
（NegativeCachingHolder ttl 可配默认 30s——双格式时长解析）；DisposableBean
关闭钩子停用（上下文关闭/测试隔离——已包装会话的缓存自然过期，无残留）。

## Testing Decisions

- `NegativeCacheYmlAssemblyTest`（ApplicationContextRunner）三断言：
  enabled+ttl 声明 → bean 存在 + 上下文关闭后 holder 停用；ttl=2m 生效；
  缺省无 bean 零行为。

## Out of Scope

- per-tool TTL 差异（全局统一——恢复窗口语义一致）。
