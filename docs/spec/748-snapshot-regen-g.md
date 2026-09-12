# 748 — API 快照再生 + api-surface.md 同步（G 会话中点收口）

> 来源：G 会话第 48 轮 = effort #748（spec 615 硬化门合规触发 + spec 645/46 惯例）/ [T1045](../../.wayfinder/tickets/T1045-snapshot-regen-shape.md) / [T1046](../../.wayfinder/tickets/T1046-snapshot-regen-verify.md) / impl 550。

## 背景

spec 705/708/709/710/711/718/719/723/746 各轮新增公共类未入档——快照门在下次全量 verify 前需再生。

## 目标

- 全量 reactor regenerate（spec 615 口径）：diff 恰 8 个新公共类（ConfigDiff/RollingMaxCounter/ForkLineageWalker/SessionExportConditional/MessageStoreContract/WebhookRateLimiter/ToolDenialLog/RouteStages），零移除零意外。
- api-surface.md 同步补 8 行（core 7 + guard 1 + resilience 1——含此前已入的 RoutingSlowStart 校对）。

## 兼容性

纯文档/快照域；生产代码零变化。
