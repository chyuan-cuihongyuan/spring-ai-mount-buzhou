# 624 — 预算池借比例上限

> 借鉴：[kubernetes](https://github.com/kubernetes/kubernetes) LimitRange limit-ratio。
> 来源：F 会话第 25 轮 = effort #600 / [T898](../../.wayfinder/tickets/T898-budget-ratio-shape.md) / [T899](../../.wayfinder/tickets/T899-budget-ratio-verify.md) / impl 477。

## 背景

弹性预算池（spec 157）surplus 借用无每会话上限——单借方可吃光全部 surplus，同伴只剩保底（突发会话饿死平稳会话的反面案例）。

## 目标

`ElasticBudgetPool(capacity, baseQuotas, borrowRatio)`：单会话 held ≤ base × ratio。

## 非目标

- 不做动态利率/惩罚性回收。
- 不接 yml（编程构造面）。

## 设计

借用路径在 surplus 检查后加 ratio 上限检查；null = 既有语义；base=0 × ratio → 不可借（诚实边界）；< 1 构造拒绝。

## 测试

3 用例：恰限放行越限拒 / 默认不设限旧语义 / 零 base 与校验。

## 兼容性

两参构造保留；默认行为零变化。
