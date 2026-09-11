# Spec 530 — per-model 预算闸（effort #530）

> wayfinder map：`.wayfinder/maps/effort-530.md`（T813–T814）。E 会话第 31 轮。

## Problem Statement

预算面有会话/虚拟 key/period 维度——模型维度空白：某模型费用失控无
独立闸。ModelCostLedger 已按模型记账（174），缺对照裁决。

## Solution

`budget.ModelBudgetGate`（BuzhouHook，order 305）：

- exhausted()：ModelCostLedger.costOf(model) ≥ yml 预算（
  `buzhou.budget.model-budget.<model>` microUsd）。
- beforeModel：耗尽 → HookResult.block（告示含模型名/已记账/预算/yml
  修法）；未达/未声明模型放行。
- 装配：map 非空 Binder **根绑定**才装配（单 Map 组件 record 构造绑定
  在 prefix.<组件名> 子路径——根前缀 yml 必须根绑定直读，409/505 同型
  待审计修复）。

## User Stories

1. 作为成本宿主，我想给每个模型设独立预算， so 单模型费用失控时下一轮
   即被拦截（结构化告示指路修法）。

## Implementation Decisions

- 以记账面为准：未喂账恒 0 恒放行（观测面驱动，非硬计数闸）。
- 阈值语义 = 已记账 ≥ 预算即耗尽（下一轮拦截，在飞不回滚）。

## Testing Decisions

- 达预算 block（告示含模型名/已记账/预算/yml 键）；未达放行；未声明
  模型放行；空表/非正预算 fail-fast；yml 装配/缺席。

## Out of Scope

- 滚动窗预算；自动降级；多模型聚合预算。

## Further Notes

- 新公共类型 `ModelBudgetGate` 随轮 regenerate 快照 + api-surface.md 加行。
