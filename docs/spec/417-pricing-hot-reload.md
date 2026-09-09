# Spec 417 — 价目热更新（effort #417）

> wayfinder map：`.wayfinder/maps/effort-417.md`（T725–T726）。D 会话第 18 轮。

## Problem Statement

价目表是 record 分量构造期定死：模型调价（供应商公告）只能重启生效；
调价日新账仍用旧价计量，账面即时失真（314 快照随单只保旧账可复算）。

## Solution

`core.budget.PricingTable`（320/340 rebind 同模式；Stripe 版本化价目即时
生效思想）：

- **持有者**：静态底表（构造期 `BuzhouTokenBudgetProperties.pricing`）+
  volatile 热载覆盖层；`of(model)` 覆盖层优先（null = 无价 → 计量 0 诚实）。
- **热载**：`ApplicationListener<BuzhouConfigRefreshEvent>` → Binder 重读
  `buzhou.token-budget.pricing` → **整表替换**覆盖层（删除键语义靠整表
  表达——旧覆盖键消失即回落底表）+ WARN 逐键 diff（旧价→新价——审计面）
  + 计数 `buzhou.pricing.reloaded`。
- **TokenBudgetHook 接线**：可选 PricingTable 注入（既有 4/5 参构造零改动；
  null = 旧行为零变化）；`microUsd` 先查表后查 props。
- 装配：PricingTable bean 恒在（空表零行为）+ listener；tokenBudgetHook
  bean 注入表。

## User Stories

1. 作为平台运维，我想供应商调价后发个刷新事件就生效，so 新账即时
   用新价不重启。
2. 作为审计，我想热载逐键 WARN 留痕，so 调价时刻与幅度可查。
3. 作为宿主，我想未刷新时行为与旧完全一致，so 升级零风险。

## Implementation Decisions

- 整表替换（非合并）——删除语义成立。
- 底表不变（props record 仍是静态事实源；覆盖层只是运行时视图）。

## Testing Decisions

- 底表查询；热载覆盖优先；再热载回落（整表替换表达删除）；
  WARN diff 行为（listener 调用后表变化）；TokenBudgetHook 接线后
  microUsd 用新价（构造注入表 vs props 差异断言）；装配 bean 在。

## Out of Scope

- PeriodBudget/预测接表；价目历史；多币种；分时价。

## Further Notes

- 新公共类型 `PricingTable` 随轮 regenerate 快照。
