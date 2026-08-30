# Wayfinder Map — Buzhou 性质测试轮（effort #134，A 会话第 29 轮）

> A 侧票号 T501+ / spec 偶数段沿用。纯测试轮（无产品代码——上会话文档轮
> 同先例）。借鉴 jqwik/QuickCheck property-based testing。

## Destination

核心不变量的随机输入验证（351 例）：签名数字折叠/长度界/无裸数字、keyOf
确定性形状、RetryBudget 守恒——「实现符合声称的数学性质」有独立证据层。

## Notes

- 固定种子 SplittableRandom(42)——失败可复现；不引新依赖（@RepeatedTest
  随机输入实现 property 语义）。

## Decisions so far

- [性质测试](tickets/T537-properties.md) — 五不变量 × 随机重复。

## Not yet specified

- 租户 id 正则不变量（拒绝任意含非法字符输入）；VirtualKeys 守恒性质。

## Out of scope

- jqwik 依赖引入（.shrink 收益 vs 依赖成本——值不值留 fog）。

## Tickets

- [x] [T537 性质测试](tickets/T537-properties.md)（impl-301）
- [x] [T538 收口提交](tickets/T538-properties-close.md)（impl-301）
