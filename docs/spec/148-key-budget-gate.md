# Spec 148 — key 级预算闸（effort #119）

> wayfinder map：`.wayfinder119/MAP.md`（T501–T502）。spec 124（VirtualKeys
> 注册表）的闸位接线。借鉴：LiteLLM virtual-key 超限即拒的闸位语义。

## Problem Statement

spec 124 落了 per-key token 配额注册表但没有闸位：扣减没人调、越限没人拦——
注册表是账本不是刹车。

## Solution

`TokenBudgetHook` 5 参构造（可选 `VirtualKeys + key`，null = 既有行为零变化）：
- **afterModel**：usage 入账后 `trySpend(key, prompt+completion)`——跨会话共享
  同一 key 额度；越限即刻发 `budget.key-hard-stop` 观测事件（本响应已生成，
  不撕毁——与会话硬顶同「不可逆预算」纪律）。
- **beforeModel**：`isExhausted(key)` 为真即拦截（模型零调用，block 文案为最终
  回复）。**耗尽态**：已用 ≥ 限额，或上次扣减越限被拒——150/200 类「部分消耗
  永远凑不满」不死循环的诚实表达；`reset(key)` 同清用量与耗尽态。

## User Stories

1. 作为平台运维，key 预算耗尽后新会话的模型调用被拦，所以单 key 失控不拖垮
   全体、且拦截发生在调用前零浪费。
2. 作为财务，窗口 reset 即恢复，所以「每窗口一份 key 账」与闸位生命周期一致。

## Testing Decisions

- e2e：入账→越限（事件 limit/value）→下一调用拦截（模型零调用）→reset 恢复；
  无 key 配置零变化。VirtualKeys 单元 + 会话预算 e2e 回归。

## Out of Scope

- autoconfig yml 键；多 key 路由；成本面配额。

## Further Notes

- 三层预算正交成体系：session 硬顶（spec 16）/ key 配额（spec 124+148）/
  会话预算观测事件同管线。
