# 703 — 健康加权路由抑制原语

> 来源：G 会话第 4 轮 = effort #703（339/340 路由 × 15 熔断跨面联动）/ [T1006](../../.wayfinder/tickets/T1006-health-dampener.md) / [T1007](../../.wayfinder/tickets/T1007-health-dampener-verify.md) / impl 603。

## Problem

加权路由（WeightedChatModel）与模型熔断器（ModelCircuitBreaker）是两个互不知情的韧性面：模型跳闸后路由仍按声明权重把流量送过去——每笔都要撞一次闸（拒绝异常+计数），权重形同虚设；恢复后也没有「权重跟着健康回来」的机制。运维手工调权（setWeight）依赖人盯告警。

## Solution

HAProxy agent-check 思想（健康检查结果反馈到 server 权重；backup 语义）：

- **监听缝**：`ModelCircuitBreaker.addTransitionListener(Consumer<Transition>)`——变迁时通知（listener 异常隔离：WARN 留痕不伤状态机；无 listener 零开销）。
- **抑制原语**：`RoutingHealthDampener.attach(router, breaker, floorWeight)`：
  - 变迁 →OPEN：`setWeight(model, floorWeight)`（默认 1——**压到地板而非零**：全模型齐跳时保留最后通道，panic-threshold 同思想）；
  - 变迁 →CLOSED：恢复声明权重（attach 时快照 `routes()` 为恢复基线）；dampened 集追踪——重复 OPEN 幂等，未压权时 CLOSED 不动权重；
  - 变迁 →HALF_OPEN：维持地板（探测 trickle——半开本就单探测）；
  - 路由候选无此名：静默忽略（breaker 模型名与路由 beanName 按名相等匹配）。
- `dampened()` 读数：当前被压权模型视图。

## User Stories

1. 故障期：模型 X 跳闸瞬间其路由权重压到 1——流量自动偏健康路，少量探测流量保留。
2. 恢复期：X 恢复 CLOSED 权重自动回声明值，无人值守。

## Implementation Decisions

- 零 yml 装配（attach 原语轮——装配级自动接线需要路由/熔断同域创建点，本面先把缝与原语立住；宿主手工装配路由场景直接可用）。
- floorWeight 必须 ≥1（构造 fail-fast——0 会造成全零黑洞）。
- 恢复基线取 attach 时刻 routes() 快照（运行期人工 setWeight 的调权会被恢复覆盖——诚实边界注记）。

## Testing Decisions

- attach 后驱动 3 失败→OPEN：被压路权重=floor、邻路不变；时钟推进+探测成功→CLOSED：恢复声明权重。
- 未匹配模型名（路由候选外）→忽略且 dampened() 不含。
- floor<1 构造拒绝；listener 抛异常→breaker 状态机照常变迁（隔离）。

## Out of Scope

- 渐变 ramp（恢复分步回权）——后续轮。
- 装配级自动接线（需要同域创建点重构）。
- 时段路由（503）窗口权重的健康叠加（两层叠加语义未定）。

## Further Notes

借鉴定源：HAProxy agent-check/weight 反馈；Envoy outlier detection 与路由联动；panic threshold（601 已有）思想复用在「地板非零」。
