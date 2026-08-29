# Spec 176 — 成本台账接线（effort #132）

> wayfinder map：`.wayfinder132/MAP.md`（T532–T533）。spec 174 fog「Hook
> 打点接线」收口。

## Problem Statement

ModelCostLedger 只有手动 record——没人喂账，台账是空转的库能力。

## Solution

`TokenBudgetHook.afterModel` 在价目换算处单点入账：`ModelCostLedger
.global().record(model, costMicroUsd)`。零配置（预算钩子默认开 = 账本默认
有账）；无价目模型记零值行（在册可见「跑过零成本」）；跨会话累计同一模型。
只记账不拦截（与硬顶闸正交）。

## User Stories

1. 作为财务，部署即有全局模型成本账（无需配置），所以账单复盘零接入成本。
2. 作为运维，无价目模型在台账零值在册，所以「忘了配价目」在账面上可见。

## Testing Decisions

- e2e：价目模型跨两会话累计（2×1050 microUsd）+ reset 清零；无价目零值在册。
  预算族回归（含 key 闸 e2e）。

## Out of Scope

- JSONL 导出；per-tenant；enabled=false 旁路记账。

## Further Notes

- 至此预算面四块闭环：session 闸 / key 配额 / 全局账本+自动入账 / 观测事件。
