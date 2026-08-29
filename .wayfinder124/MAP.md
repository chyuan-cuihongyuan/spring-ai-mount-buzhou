# Wayfinder Map — Buzhou 基数守卫装配（effort #124，A 会话第 19 轮）

> A 侧票号 T501+ / spec 偶数段沿用。spec 132 fog「默认装配接入」收口。

## Destination

buzhou.metrics.cardinality-guard.enabled 一键开全局守卫：Holder 安装面装饰
TagCardinalityGuard（默认关零变化）。

## Notes

- 安装点 = BuzhouMetricsConfiguration.buzhouMetricsHolderInstaller（唯一
  全局 metrics 装配处）；metadata+矩阵登记三件套。

## Decisions so far

- [守卫装配](tickets/T513-guard-install.md) — env 直读 Boolean 默认 false +
  装饰后安装。

## Not yet specified

- folds() 进健康/日志告警面；per-tag-key 差异化封顶配置。

## Out of scope

- 守卫自身进 registry；热开关。

## Tickets

- [x] [T513 守卫装配](tickets/T513-guard-install.md)（impl-291）
- [x] [T514 收口提交](tickets/T514-guard-install-close.md)（impl-291）
