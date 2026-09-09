# Wayfinder Map — Buzhou 价目热更新（effort #417，D 会话第 18 轮）

> D 会话第 18 轮。勘察（2026-09-08）：价目表 `buzhou.token-budget.pricing`
> 是 **BuzhouTokenBudgetProperties record 分量——构造期定死**；模型调价
> （供应商公告）只能重启生效；320/340 的 BuzhouConfigRefreshEvent 热重载
> 模式没有覆盖价目。成本计量对价目过期的敏感性高（调价日账面即失真，
> 314 价目快照随单只能让旧账可复算，不能让新账用新价）。

## Destination

`core.budget.PricingTable`（320/340 rebind 同模式收价款）：可变价目持有
者——静态底表（构造期 properties）+ 热载覆盖层（BuzhouConfigRefreshEvent
→ Binder 重读 `buzhou.token-budget.pricing` → 整表替换 + WARN 日志逐键
变更 + 计数 `buzhou.pricing.reloaded`）；`of(model)` 覆盖层优先。
TokenBudgetHook 增可选 PricingTable 注入（既有构造零改动；null = 旧行为
零变化）；microUsd 先查表。bean 恒在（空表零行为）+ 事件 listener。

## Notes

- 号段：spec 417 / T725–T726 / impl-390。
- 借鉴源：Spring Cloud rebind（320/340 先例）；Stripe 版本化价目表
> （调价即时生效不重启）思想。
- 纪律：热载是**整表替换**（非逐键合并——删除键的语义靠整表表达）；
  WARN 逐键 diff（旧价→新价——审计面）；PeriodBudgetHook 接表为扩散候选。

## Out of scope

- PeriodBudgetHook/成本预测接表（扩散候选）；价目历史保留（314 快照随单
  已管账面历史）；多币种；分时价。

## Tickets

- [x] [T725 PricingTable + 热载](../tickets/T725-pricing-table.md)
- [x] [T726 TokenBudgetHook 接线 + 装配](../tickets/T726-pricing-wiring.md)
