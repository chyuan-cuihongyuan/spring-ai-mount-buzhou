# Wayfinder Map — Buzhou 期望门禁接线（effort #120，A 会话第 15 轮）

> A 侧票号 T501+ / spec 偶数段沿用。spec 134 的闸位接线（同 spec148 模式：
> 账本→刹车）。

## Destination

EvalRunner run 前期望门禁：脏数据集 fail-fast（模型零调用——脏数据零 token
成本出局），message 带 summary + 前三条发现；未装载零变化。

## Decisions so far

- [EvalRunner 门禁](tickets/T503-runner-gate.md) — setExpectations 可选装载 +
  run() items 加载后校验，未过挂 EVAL_OPERATION_INVALID。

## Not yet specified

- 「警告不拦截」宽严两档；门禁结果随 run 落档（EvalRunResult 列）。

## Out of scope

- 期望套件随数据集持久化；CI 门禁输出格式（-t junit 之类）。

## Tickets

- [x] [T503 期望门禁接线](tickets/T503-runner-gate.md)（impl-287）
- [x] [T504 收口提交](tickets/T504-runner-gate-close.md)（impl-287）
