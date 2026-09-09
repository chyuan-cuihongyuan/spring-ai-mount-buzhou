# Wayfinder Map — Buzhou 价目快照随单（effort #314，C 会话第 15 轮）

> C 会话第 15 轮。成本台账只有金额——价目（buzhou.token-budget.pricing）改了
> 之后，历史账单无法复算（不知道记账时的单价）；合规对账缺「当时价」事实
> （fog 152「价目快照随单」项）。

## Destination

`ModelCostLedger` 行级价目快照：record 时随单记录该模型单价（input/output
perMillion）；JSONL 账单行自含单价两列——价目变更后旧账仍可复算（复式
记账审计口径）。旧签名/无价目路径逐位兼容。

## Notes

- 号段：spec 314 / T619–T620 / impl-337。
- 借鉴：复式记账（账单自含计价事实——审计可离线复算）。

## Decisions so far

- 快照取「该模型最近一次记账时」的单价（混价窗口以最后价为示——诚实
  边界：行级逐笔记价归事件流，台账是聚合面）。
- PricingSnapshot(inputPerMillion, outputPerMillion) BigDecimal 原口径。

## Out of scope

- 逐笔记价事件流（budget.tokens-accumulated 已有金额，加价版本归后续）；
- 价目历史版本表（reloadable 163 族的快照订阅）。

## Tickets

- [x] [T619 行级价目快照 + record 三参与旧签名兼容](../tickets/T619-pricing-snapshot.md)（impl-337）
- [x] [T620 JSONL 单价列 + TokenBudgetHook 接线回归](../tickets/T620-pricing-close.md)（impl-337）
