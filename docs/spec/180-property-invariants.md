# Spec 180 — 性质测试（effort #134）

> wayfinder map：`.wayfinder134/MAP.md`（T537–T538）。纯测试轮。借鉴：
> jqwik/QuickCheck property-based testing（随机输入 × 声称的不变量）。

## Problem Statement

核心算法（签名归一/键派生/预算账）的既有测试是例证式（example-based）——
性质声明（「仅数字不同必同族」）只在被测的几个手工例子里验证过，随机输入
下的边角无独立证据层。

## Solution

`PropertyInvariantsTest`（@RepeatedTest 随机输入实现 property 语义，固定
种子 42 可复现，零新依赖）：五不变量——①仅数字段不同的消息同签名；②签名
长度有界（kind + 96）；③归一签名无裸数字；④keyOf 确定性 + 64 hex 形状；
⑤RetryBudget 守恒（放行 + 拒绝 = 尝试数，余额非负）。共 351 例随机验证。

## User Stories

1. 作为维护者，重构这些算法时性质层先炸（例证层可能恰好都过），所以回归
   网多一道独立防线。
2. 作为审计者，「实现符合声称的数学性质」有随机化证据，不是只挑好的例子。

## Testing Decisions

- 本轮即测试：351 例全绿（0.2s——性质层廉价可常跑）。

## Out of Scope

- jqwik 引入与 shrink；更多被测面（租户正则/虚拟键守恒——fog）。

## Further Notes

- 与红队（对抗恶意输入）互补：性质测「承诺永真」，红队测「恶意必拒」。
